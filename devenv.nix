{ pkgs, config, lib, ... }:

let
  # On NixOS the GPU drivers (libGL/EGL, Vulkan ICDs) live here instead of the nix store.
  driverDir = "/run/opengl-driver";
  # Everything the game dlopen()s at runtime must be in LD_LIBRARY_PATH: the graphics stack,
  # the audio backends of its bundled OpenAL (pulse/pipewire, else sound dies with
  # "Failed to open OpenAL device") and the libs SDL3 needs for windowing.
  runtimeLibs = lib.makeLibraryPath (with pkgs; [
    libglvnd
    mesa
    vulkan-loader
    libpulseaudio
    pipewire
    libx11
    libxcursor
    libxrandr
    libxi
    libxinerama
    libxxf86vm
    libxkbcommon
    wayland
    alsa-lib
  ]);
in
{
  # Minecraft 26.x requires Java 25.
  languages.java = {
    enable = true;
    jdk.package = pkgs.jdk25;
    lsp.enable = false;
  };

  packages = with pkgs; [
    # OpenGL and Vulkan (loader + ICD discovery) for the dev client
    libglvnd
    mesa # llvmpipe/lavapipe software fallback
    vulkan-loader
    vulkan-tools
    # windowing/input that Minecraft's SDL3 and OpenAL need
    libx11
    libxcursor
    libxrandr
    libxi
    libxinerama
    libxxf86vm
    libxkbcommon
    wayland
    # audio (OpenAL): the game's bundled OpenAL dlopens the pulse/pipewire client libs
    alsa-lib
    libpulseaudio
    pipewire
  ];

  env.GRADLE_USER_HOME = "${config.devenv.root}/.gradle-home";

  enterShell = ''
    # NixOS keeps the drivers out of the nix store, so they have to be added to the
    # library path by hand (this is what lets OpenGL/Vulkan work inside the shell).
    export LD_LIBRARY_PATH="${driverDir}/lib:${runtimeLibs}:$LD_LIBRARY_PATH"
    export XDG_DATA_DIRS="${driverDir}/share:$XDG_DATA_DIRS"
  '';
}
