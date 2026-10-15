package net.iskaa303.dividedchunks;

import net.iskaa303.dividedchunks.core.config.DividedChunksConfig;
import net.iskaa303.dividedchunks.platform.Platform;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//? fabric {
/*import net.iskaa303.dividedchunks.platform.fabric.FabricPlatform;
*///?} neoforge {
import net.iskaa303.dividedchunks.platform.neoforge.NeoforgePlatform;
 //?}

public class DividedChunks {

	public static final String MOD_ID = /*$ mod_id*/ "dividedchunks";
	public static final String MOD_VERSION = /*$ mod_version*/ "0.0.1";
	public static final String MOD_FRIENDLY_NAME = /*$ mod_name*/ "DividedChunks";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final Platform PLATFORM = createPlatformInstance();
	private static DividedChunksConfig config = new DividedChunksConfig();

	public static void onInitialize() {
		LOGGER.info("Initializing {} {}", MOD_ID, MOD_VERSION);
		config = DividedChunksConfig.load(PLATFORM.configDir());
		LOGGER.info("Config: {}", config.describe());
	}

	public static void onInitializeClient() {
		LOGGER.info("Initializing {} {} client", MOD_ID, MOD_VERSION);
	}

	public static Platform xplat() {
		return PLATFORM;
	}

	public static DividedChunksConfig config() {
		return config;
	}

	private static Platform createPlatformInstance() {
		//? fabric {
		/*return new FabricPlatform();
		*///?} neoforge {
		return new NeoforgePlatform();
		 //?}
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
