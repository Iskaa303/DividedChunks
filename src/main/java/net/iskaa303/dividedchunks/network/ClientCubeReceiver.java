package net.iskaa303.dividedchunks.network;

import net.iskaa303.dividedchunks.core.world.ColumnSectionAccess;
import net.iskaa303.dividedchunks.core.world.SectionCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.LevelChunk;

/** Applies a received cube section to the client's sparse column cache. */
public final class ClientCubeReceiver {

	private ClientCubeReceiver() {
	}

	public static void handle(CubePayload payload) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null || !level.getChunkSource().hasChunk(payload.chunkX(), payload.chunkZ())) {
			return;
		}
		LevelChunk chunk = level.getChunk(payload.chunkX(), payload.chunkZ());
		ColumnSectionAccess access = (ColumnSectionAccess) chunk;
		if (payload.data().length == 0) {
			access.dividedchunks$removeSection(payload.sectionY());
		} else {
			access.dividedchunks$putSection(payload.sectionY(),
					SectionCodec.decode(payload.data(), level.palettedContainerFactory()));
		}
	}
}
