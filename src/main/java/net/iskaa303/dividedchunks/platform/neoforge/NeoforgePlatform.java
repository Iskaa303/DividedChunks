package net.iskaa303.dividedchunks.platform.neoforge;

//? neoforge {

import net.iskaa303.dividedchunks.network.CubePayload;
import net.iskaa303.dividedchunks.platform.Platform;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.Path;

public class NeoforgePlatform implements Platform {

	@Override
	public boolean isModLoaded(String modId) {
		return ModList.get().isLoaded(modId);
	}

	@Override
	public ModLoader loader() {
		return ModLoader.NEOFORGE;
	}

	@Override
	public String mcVersion() {
		return FMLLoader.getCurrent().getVersionInfo().mcVersion();
	}

	@Override
	public boolean isDevelopmentEnvironment() {
		return !FMLLoader.getCurrent().isProduction();
	}

	@Override
	public Path configDir() {
		return FMLPaths.CONFIGDIR.get();
	}

	@Override
	public void sendToPlayer(ServerPlayer player, CubePayload payload) {
		PacketDistributor.sendToPlayer(player, payload);
	}

	@Override
	public void sendToTracking(ServerLevel level, ChunkPos pos, CubePayload payload) {
		PacketDistributor.sendToPlayersTrackingChunk(level, pos, payload);
	}
}
//?}
