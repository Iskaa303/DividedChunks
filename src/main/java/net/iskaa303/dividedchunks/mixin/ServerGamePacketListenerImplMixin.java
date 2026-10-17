package net.iskaa303.dividedchunks.mixin;

import net.iskaa303.dividedchunks.DividedChunks;
import net.iskaa303.dividedchunks.core.config.DividedChunksConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Server-side block interactions are gated by the level's {@code getMinY}/{@code getMaxY}, which
 * stay on the vanilla band, so vanilla drops an out-of-band click as "too high" before it reaches
 * the chunk and only {@code /setblock} (gated on the widened {@code isInValidBounds}) gets through.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

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
