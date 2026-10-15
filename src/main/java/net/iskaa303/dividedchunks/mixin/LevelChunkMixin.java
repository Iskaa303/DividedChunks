package net.iskaa303.dividedchunks.mixin;

import net.iskaa303.dividedchunks.core.world.ColumnSectionAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Handles block read/write outside the dimension's section band through the sparse column storage.
 * Inside the band the vanilla implementation runs unchanged (it already goes through
 * {@code getSection}); outside it, the section is created on demand and the heightmap/light side
 * effects are skipped.
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {

	@Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
	private void dividedchunks$getBlockState(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
		LevelChunk self = (LevelChunk) (Object) this;
		if (this.dividedchunks$inBand(self, pos.getY())) {
			return;
		}
		LevelChunkSection section = ((ColumnSectionAccess) self)
				.dividedchunks$getSectionByY(SectionPos.blockToSectionCoord(pos.getY()));
		cir.setReturnValue(section == null || section.hasOnlyAir()
				? Blocks.AIR.defaultBlockState()
				: section.getBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15));
	}

	@Inject(method = "getFluidState(III)Lnet/minecraft/world/level/material/FluidState;",
			at = @At("HEAD"), cancellable = true)
	private void dividedchunks$getFluidState(int x, int y, int z, CallbackInfoReturnable<FluidState> cir) {
		LevelChunk self = (LevelChunk) (Object) this;
		if (this.dividedchunks$inBand(self, y)) {
			return;
		}
		LevelChunkSection section = ((ColumnSectionAccess) self).dividedchunks$getSectionByY(SectionPos.blockToSectionCoord(y));
		cir.setReturnValue(section == null || section.hasOnlyAir()
				? Fluids.EMPTY.defaultFluidState()
				: section.getFluidState(x & 15, y & 15, z & 15));
	}

	@Inject(method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Lnet/minecraft/world/level/block/state/BlockState;",
			at = @At("HEAD"), cancellable = true)
	private void dividedchunks$setBlockState(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> cir) {
		LevelChunk self = (LevelChunk) (Object) this;
		if (this.dividedchunks$inBand(self, pos.getY())) {
			return;
		}

		ColumnSectionAccess access = (ColumnSectionAccess) self;
		int sectionY = SectionPos.blockToSectionCoord(pos.getY());
		LevelChunkSection section = access.dividedchunks$getSectionByY(sectionY);
		if (section == null) {
			section = new LevelChunkSection(self.getLevel().palettedContainerFactory());
			access.dividedchunks$putSection(sectionY, section);
		}

		boolean wasEmpty = section.hasOnlyAir();
		if (wasEmpty && state.isAir()) {
			cir.setReturnValue(null);
			return;
		}

		BlockState oldState = section.setBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, state);
		if (oldState == state) {
			cir.setReturnValue(null);
			return;
		}

		self.markUnsaved();
		cir.setReturnValue(oldState);
	}

	private boolean dividedchunks$inBand(LevelChunk self, int blockY) {
		int sectionY = SectionPos.blockToSectionCoord(blockY);
		return sectionY >= self.getMinSectionY() && sectionY <= self.getMaxSectionY();
	}
}
