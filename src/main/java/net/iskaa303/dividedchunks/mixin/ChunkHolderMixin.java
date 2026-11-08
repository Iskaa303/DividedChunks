package net.iskaa303.dividedchunks.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.LevelHeightAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@link ChunkHolder} tracks changed blocks in a dense per-section array sized to the section band.
 * Changes outside that band are skipped here; they reach clients as a cube payload instead.
 */
@Mixin(ChunkHolder.class)
public abstract class ChunkHolderMixin {

	@Shadow
	@Final
	private LevelHeightAccessor levelHeightAccessor;

	@Inject(method = "blockChanged", at = @At("HEAD"), cancellable = true)
	private void dividedchunks$blockChanged(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		int sectionY = SectionPos.blockToSectionCoord(pos.getY());
		if (sectionY < this.levelHeightAccessor.getMinSectionY()
				|| sectionY > this.levelHeightAccessor.getMaxSectionY()) {
			cir.setReturnValue(false);
		}
	}
}
