package net.iskaa303.dividedchunks.network;

import net.iskaa303.dividedchunks.DividedChunks;
import net.iskaa303.dividedchunks.core.world.SectionCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jetbrains.annotations.Nullable;

/** Server-side helpers for sending cube sections to clients. */
public final class CubeSync {

	private CubeSync() {
	}

	/** Sends one section to every player tracking the chunk; a {@code null} section removes it there. */
	public static void broadcast(ServerLevel level, ChunkPos pos, int sectionY, @Nullable LevelChunkSection section) {
		DividedChunks.xplat().sendToTracking(level, pos, payload(level, pos, sectionY, section));
	}

	/** Sends one section to a single player; a {@code null} section removes it there. */
	public static void send(ServerPlayer player, ServerLevel level, ChunkPos pos, int sectionY,
			@Nullable LevelChunkSection section) {
		DividedChunks.xplat().sendToPlayer(player, payload(level, pos, sectionY, section));
	}

	private static CubePayload payload(ServerLevel level, ChunkPos pos, int sectionY,
			@Nullable LevelChunkSection section) {
		byte[] data = section == null || section.hasOnlyAir()
				? new byte[0]
				: SectionCodec.encode(section, level.palettedContainerFactory());
		return new CubePayload(pos.x(), sectionY, pos.z(), data);
	}
}
