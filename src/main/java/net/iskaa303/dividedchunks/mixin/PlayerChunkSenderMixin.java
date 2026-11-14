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
 * Sends a column's sky occluder tops right behind its chunk. The tops outlive the client's section
 * window, so a chunk that is reloaded on the client keeps shading the band under a cube far above.
 */
@Mixin(PlayerChunkSender.class)
public abstract class PlayerChunkSenderMixin {

	@Inject(method = "sendChunk", at = @At("TAIL"))
	private static void dividedchunks$sendTopsWithChunk(ServerGamePacketListenerImpl connection, ServerLevel level,
			LevelChunk chunk, CallbackInfo ci) {
		CubeSync.sendTops(connection.player, level, chunk.getPos());
	}
}
