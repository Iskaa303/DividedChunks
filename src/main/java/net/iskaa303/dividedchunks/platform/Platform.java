package net.iskaa303.dividedchunks.platform;

import java.nio.file.Path;

public interface Platform {
	boolean isModLoaded(String modId);

	ModLoader loader();

	String mcVersion();

	boolean isDevelopmentEnvironment();

	/** Directory where the mod keeps its config file, provided by the loader. */
	Path configDir();

	enum ModLoader {
		FABRIC, NEOFORGE
	}
}
