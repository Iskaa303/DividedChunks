package net.iskaa303.dividedchunks.mixin;

import net.iskaa303.dividedchunks.network.CubeSync;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sends a chunk's stored out-of-band sections right after the chunk packet, so a client joining or
 * moving near existing high-Y builds receives them.
 */
@Mixin(PlayerChunkSender.class)
public abstract class PlayerChunkSenderMixin {

	@Inject(method = "sendChunk", at = @At("RETURN"))
	private static void dividedchunks$sendStoredCubes(ServerGamePacketListenerImpl connection, ServerLevel level,
			LevelChunk chunk, CallbackInfo ci) {
		CubeSync.sendColumn(connection.player, level, chunk);
	}
}
