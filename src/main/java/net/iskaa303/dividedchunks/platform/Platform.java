package net.iskaa303.dividedchunks.platform;

import net.iskaa303.dividedchunks.network.CubePayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.nio.file.Path;

public interface Platform {
	boolean isModLoaded(String modId);

	ModLoader loader();

	String mcVersion();

	boolean isDevelopmentEnvironment();

	/** Directory where the mod keeps its config file, provided by the loader. */
	Path configDir();

	/** Sends one cube section to a single player. */
	void sendToPlayer(ServerPlayer player, CubePayload payload);

	/** Sends one cube section to every player tracking {@code pos}. */
	void sendToTracking(ServerLevel level, ChunkPos pos, CubePayload payload);

	enum ModLoader {
		FABRIC, NEOFORGE
	}
}
