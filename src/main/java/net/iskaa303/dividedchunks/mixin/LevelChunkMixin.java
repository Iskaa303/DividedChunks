package net.iskaa303.dividedchunks.mixin;

import net.iskaa303.dividedchunks.api.util.CubePos;
import net.iskaa303.dividedchunks.core.storage.CubeRegionStorage;
import net.iskaa303.dividedchunks.core.world.ColumnSectionAccess;
import net.iskaa303.dividedchunks.core.world.CubeStores;
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

import java.util.Map;

/**
 * Reads and writes blocks outside the dimension's section band through the sparse column storage:
 * the section is created on demand, and the heightmap/light side effects are skipped. Stored
 * out-of-band sections load lazily on first access and are written back when they change.
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {

	@Unique
	private boolean dividedchunks$diskLoaded;

	@Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
	private void dividedchunks$getBlockState(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
		LevelChunk self = (LevelChunk) (Object) this;
		if (this.dividedchunks$inBand(self, pos.getY())) {
			return;
		}
		this.dividedchunks$loadStoredSections(self);
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
		this.dividedchunks$loadStoredSections(self);
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
		this.dividedchunks$loadStoredSections(self);

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
		this.dividedchunks$persist(self, section, sectionY);
		cir.setReturnValue(oldState);
	}

	@Unique
	private void dividedchunks$loadStoredSections(LevelChunk self) {
		if (this.dividedchunks$diskLoaded) {
			return;
		}
		this.dividedchunks$diskLoaded = true;
		if (!(self.getLevel() instanceof ServerLevel serverLevel)) {
			return;
		}
		Map<Integer, byte[]> stored = CubeStores.get(serverLevel).readColumn(self.getPos().x(), self.getPos().z());
		if (stored.isEmpty()) {
			return;
		}
		ColumnSectionAccess access = (ColumnSectionAccess) self;
		for (Map.Entry<Integer, byte[]> entry : stored.entrySet()) {
			access.dividedchunks$putSection(entry.getKey(),
					SectionCodec.decode(entry.getValue(), serverLevel.palettedContainerFactory()));
		}
	}

	@Unique
	private void dividedchunks$persist(LevelChunk self, LevelChunkSection section, int sectionY) {
		if (!(self.getLevel() instanceof ServerLevel serverLevel)) {
			return;
		}
		var chunkPos = self.getPos();
		CubePos pos = new CubePos(chunkPos.x(), sectionY, chunkPos.z());
		CubeRegionStorage storage = CubeStores.get(serverLevel);
		byte[] data = section.hasOnlyAir() ? new byte[0] : SectionCodec.encode(section, serverLevel.palettedContainerFactory());
		if (data.length == 0) {
			storage.remove(pos);
		} else {
			storage.put(pos, data);
		}
		CubeSync.broadcast(serverLevel, chunkPos, new CubePayload(chunkPos.x(), sectionY, chunkPos.z(), data));
	}

	private boolean dividedchunks$inBand(LevelChunk self, int blockY) {
		int sectionY = SectionPos.blockToSectionCoord(blockY);
		return sectionY >= self.getMinSectionY() && sectionY <= self.getMaxSectionY();
	}
}
