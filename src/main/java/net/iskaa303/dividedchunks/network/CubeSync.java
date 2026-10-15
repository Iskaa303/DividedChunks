package net.iskaa303.dividedchunks.network;

import net.iskaa303.dividedchunks.DividedChunks;
import net.iskaa303.dividedchunks.core.world.CubeStores;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Map;

/** Server-side helpers for sending cube sections to clients. */
public final class CubeSync {

	private CubeSync() {
	}

	/** Sends a single section change to every player tracking the chunk. */
	public static void broadcast(ServerLevel level, ChunkPos pos, CubePayload payload) {
		DividedChunks.xplat().sendToTracking(level, pos, payload);
	}

	/** Sends every stored section of {@code chunk} to one player. */
	public static void sendColumn(ServerPlayer player, ServerLevel level, LevelChunk chunk) {
		ChunkPos pos = chunk.getPos();
		for (Map.Entry<Integer, byte[]> entry : CubeStores.get(level).readColumn(pos.x(), pos.z()).entrySet()) {
			DividedChunks.xplat().sendToPlayer(player, new CubePayload(pos.x(), entry.getKey(), pos.z(), entry.getValue()));
		}
	}
}
