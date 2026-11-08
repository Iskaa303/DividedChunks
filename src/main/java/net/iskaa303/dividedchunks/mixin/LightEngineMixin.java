package net.iskaa303.dividedchunks.mixin;

import net.iskaa303.dividedchunks.core.world.ColumnSectionAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.lighting.SkyLightEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Answers sky light outside the vanilla band, which vanilla stores no data for, from the column's
 * cached section light.
 */
@Mixin(LightEngine.class)
public abstract class LightEngineMixin {

	@Shadow
	@Final
	protected LightChunkGetter chunkSource;

	@Inject(method = "getLightValue(Lnet/minecraft/core/BlockPos;)I", at = @At("RETURN"), cancellable = true)
	private void dividedchunks$outOfBandSkyLight(BlockPos pos, CallbackInfoReturnable<Integer> cir) {
		if (cir.getReturnValue() != 15 || !((Object) this instanceof SkyLightEngine)) {
			return;
		}
		BlockGetter blockGetter = this.chunkSource.getLevel();
		if (!(blockGetter instanceof Level level)) {
			return;
		}
		int sectionY = SectionPos.blockToSectionCoord(pos.getY());
		if (sectionY >= level.getMinSectionY() && sectionY <= level.getMaxSectionY()) {
			return;
		}
		LevelChunk chunk = level.getChunkSource()
				.getChunkNow(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
		if (chunk == null) {
			return;
		}
		byte[] light = ((ColumnSectionAccess) chunk).dividedchunks$skyLight(sectionY);
		if (light == null) {
			return;
		}
		int value = light[(pos.getY() & 15) << 8 | (pos.getZ() & 15) << 4 | (pos.getX() & 15)];
		if (value < cir.getReturnValue()) {
			cir.setReturnValue((int) value);
		}
	}
}
