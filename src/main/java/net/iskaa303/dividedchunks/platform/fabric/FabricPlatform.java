package net.iskaa303.dividedchunks.platform.fabric;

//? fabric {

/*import net.iskaa303.dividedchunks.network.CubePayload;
import net.iskaa303.dividedchunks.platform.Platform;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.nio.file.Path;

public class FabricPlatform implements Platform {

	@Override
	public boolean isModLoaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}

	@Override
	public ModLoader loader() {
		return ModLoader.FABRIC;
	}

	@Override
	public String mcVersion() {
		return FabricLoader.getInstance().getRawGameVersion();
	}

	@Override
	public boolean isDevelopmentEnvironment() {
		return FabricLoader.getInstance().isDevelopmentEnvironment();
	}

	@Override
	public Path configDir() {
		return FabricLoader.getInstance().getConfigDir();
	}

	@Override
	public void sendToPlayer(ServerPlayer player, CubePayload payload) {
		ServerPlayNetworking.send(player, payload);
	}

	@Override
	public void sendToTracking(ServerLevel level, ChunkPos pos, CubePayload payload) {
		for (ServerPlayer player : PlayerLookup.tracking(level, pos)) {
			ServerPlayNetworking.send(player, payload);
		}
	}
}
*///?}
