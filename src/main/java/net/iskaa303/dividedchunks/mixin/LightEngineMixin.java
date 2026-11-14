package net.iskaa303.dividedchunks.mixin;

import net.iskaa303.dividedchunks.core.world.ColumnSectionAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.lighting.SkyLightEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Answers sky and block light from the column's own light, which is the only data that exists outside
 * the vanilla band and the only one that knows about cubes above it.
 */
@Mixin(LightEngine.class)
public abstract class LightEngineMixin {

	@Shadow
	@Final
	protected LightChunkGetter chunkSource;

	@Inject(method = "getLightValue(Lnet/minecraft/core/BlockPos;)I", at = @At("RETURN"), cancellable = true)
	private void dividedchunks$outOfBandLight(BlockPos pos, CallbackInfoReturnable<Integer> cir) {
		BlockGetter blockGetter = this.chunkSource.getLevel();
		if (!(blockGetter instanceof Level level)) {
			return;
		}
		int sectionY = SectionPos.blockToSectionCoord(pos.getY());
		boolean inBand = sectionY >= level.getMinSectionY() && sectionY <= level.getMaxSectionY();
		LevelChunk chunk = level.getChunkSource()
				.getChunkNow(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
		if (chunk == null) {
			return;
		}
		int index = (pos.getY() & 15) << 8 | (pos.getZ() & 15) << 4 | (pos.getX() & 15);
		ColumnSectionAccess access = (ColumnSectionAccess) chunk;
		if ((Object) this instanceof SkyLightEngine) {
			// In band vanilla is exact unless a cube above the band casts a shadow, which vanilla never sees.
			if (inBand && access.dividedchunks$skyOccluderTop(pos.getX() & 15, pos.getZ() & 15) <= level.getMaxY()) {
				return;
			}
			byte[] light = access.dividedchunks$skyLight(sectionY);
			if (light != null) {
				cir.setReturnValue(Math.min(cir.getReturnValue(), light[index] & 0xFF));
			}
		} else if ((Object) this instanceof BlockLightEngine) {
			// Vanilla reports no block light out of band, so the column's value raises it.
			if (inBand) {
				return;
			}
			byte[] light = access.dividedchunks$blockLight(sectionY);
			if (light != null) {
				cir.setReturnValue(Math.max(cir.getReturnValue(), light[index] & 0xFF));
			}
		}
	}
}
