package net.iskaa303.dividedchunks.mixin;

import net.iskaa303.dividedchunks.core.server.CubeTracker;
import net.minecraft.server.level.ServerPlayer;
import net.iskaa303.dividedchunks.DividedChunks;
import net.iskaa303.dividedchunks.core.config.DividedChunksConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Server-side block interactions are gated by the level's {@code getMinY}/{@code getMaxY}, which
 * stay on the vanilla band, so vanilla drops an out-of-band click as "too high" before it reaches
 * the chunk and only {@code /setblock} (gated on the widened {@code isInValidBounds}) gets through.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

	/** Cubes this client holds, kept to the box around its player by one tick per connection. */
	@Unique
	private final CubeTracker dividedchunks$cubes = new CubeTracker();

	@Inject(method = "tick", at = @At("RETURN"))
	private void dividedchunks$tickCubes(CallbackInfo ci) {
		ServerPlayer player = ((ServerGamePacketListenerImpl) (Object) this).player;
		if (player != null) {
			this.dividedchunks$cubes.tick(player, player.level());
		}
	}

	@Redirect(
			method = "handleUseItemOn",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getMaxY()I"))
	private int dividedchunks$useMaxY(ServerLevel level) {
		return dividedchunks$config(level).maxY;
	}

	@Redirect(
			method = "handleUseItemOn",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getMinY()I"))
	private int dividedchunks$useMinY(ServerLevel level) {
		return dividedchunks$config(level).minY;
	}

	@Redirect(
			method = "handlePlayerAction",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getMaxY()I"))
	private int dividedchunks$breakMaxY(ServerLevel level) {
		return dividedchunks$config(level).maxY;
	}

	@Unique
	private static DividedChunksConfig.DimensionConfig dividedchunks$config(ServerLevel level) {
		return DividedChunks.config().forDimension(level.dimension().identifier().toString());
	}
}
