package net.iskaa303.dividedchunks.platform;

public interface Platform {
	boolean isModLoaded(String modId);

	ModLoader loader();

	String mcVersion();

	boolean isDevelopmentEnvironment();

	enum ModLoader {
		FABRIC, NEOFORGE
	}
}
