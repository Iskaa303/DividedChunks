plugins {
	id("mod-platform")
	id("dev.kikugie.loom-back-compat")
}

platform {
	loader = "fabric"
	dependencies {
		required("minecraft") {
			fabricLikeVersionRange = prop("deps.minecraft")
		}
		required("fabric-api") {
			slug("fabric-api")
			fabricLikeVersionRange = ">=${prop("deps.fabric-api")}"
		}
		required("fabricloader") {
			fabricLikeVersionRange = ">=${prop("deps.fabric-loader")}"
		}
	}
}

loom {
	accessWidenerPath = rootProject.file("src/main/resources/aw/${sc.current.version}.accesswidener")
	val devJvmArgs = propsList("mod", "dev_jvm_args")
	runs.named("client") {
		client()
		generateRunConfig.set(true)
		runDirectory.set(layout.projectDirectory.dir("run"))
		programArguments.add("--username=Dev")
		jvmArguments.addAll(devJvmArgs)
		displayName.set("Fabric Client (${sc.current.version})")
	}
	runs.named("server") {
		server()
		generateRunConfig.set(true)
		runDirectory.set(layout.projectDirectory.dir("run"))
		jvmArguments.addAll(devJvmArgs)
		displayName.set("Fabric Server (${sc.current.version})")
	}
}

fabricApi {
	configureDataGeneration {
		outputDirectory = file("${rootDir}/versions/datagen/${sc.current.version}/src/main/generated")
		client = true
	}
}

repositories {
	mavenCentral()
	strictMaven("https://maven.terraformersmc.com/", "com.terraformersmc") { name = "TerraformersMC" }
}

configurations.all {
	resolutionStrategy {
		force("net.fabricmc:fabric-loader:${prop("deps.fabric-loader")}")
	}
}

dependencies {
	minecraft("com.mojang:minecraft:${prop("deps.minecraft")}")
	modImplementation("net.fabricmc:fabric-loader:${prop("deps.fabric-loader")}")
	modImplementation("net.fabricmc.fabric-api:fabric-api:${prop("deps.fabric-api")}")
	// Dev-only: Mod Menu gives Fabric a mods list screen (NeoForge has one built in). It is not written
	// into the generated fabric.mod.json, so it stays a local convenience dependency.
	modLocalRuntime("com.terraformersmc:modmenu:${prop("deps.modmenu")}")
}
