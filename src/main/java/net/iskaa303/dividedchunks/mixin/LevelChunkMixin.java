package net.iskaa303.dividedchunks.mixin;

import net.iskaa303.dividedchunks.core.world.ColumnSectionAccess;
import net.iskaa303.dividedchunks.core.world.SectionCodec;
import net.iskaa303.dividedchunks.network.CubePayload;
import net.iskaa303.dividedchunks.network.CubeSync;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Reads and writes blocks outside the dimension's section band through the sparse column storage,
 * skipping the heightmap and light side effects; clients get the change as a cube payload.
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
		((ColumnSectionAccess) self).dividedchunks$invalidateSkyAround(pos.getY());
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
		this.dividedchunks$broadcast(self, section, sectionY);
		cir.setReturnValue(oldState);
	}

	@Unique
	private void dividedchunks$broadcast(LevelChunk self, LevelChunkSection section, int sectionY) {
		if (!(self.getLevel() instanceof ServerLevel serverLevel)) {
			return;
		}
		var chunkPos = self.getPos();
		byte[] data = section.hasOnlyAir() ? new byte[0] : SectionCodec.encode(section, serverLevel.palettedContainerFactory());
		CubeSync.broadcast(serverLevel, chunkPos, new CubePayload(chunkPos.x(), sectionY, chunkPos.z(), data));
	}

	private boolean dividedchunks$inBand(LevelChunk self, int blockY) {
		int sectionY = SectionPos.blockToSectionCoord(blockY);
		return sectionY >= self.getMinSectionY() && sectionY <= self.getMaxSectionY();
	}
}
