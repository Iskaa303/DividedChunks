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
 * Block changes outside that band are skipped here (there is nothing for the client to receive yet).
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
