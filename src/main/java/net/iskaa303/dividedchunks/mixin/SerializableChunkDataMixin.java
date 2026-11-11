package net.iskaa303.dividedchunks.mixin;

import net.iskaa303.dividedchunks.core.storage.CubeRegionStorage;
import net.iskaa303.dividedchunks.core.world.ColumnPersistence;
import net.iskaa303.dividedchunks.core.world.ColumnSectionAccess;
import net.iskaa303.dividedchunks.core.world.CubeStores;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

/**
 * Writes every section of a chunk column to the {@code .dcr} store, out of the chunk tag.
 * Sections are copied in {@code copyOf} and encoded in {@code write}, which runs on the IO worker.
 */
@Mixin(SerializableChunkData.class)
public abstract class SerializableChunkDataMixin {

	/** Store of the dimension the chunk belongs to, from {@code copyOf}. */
	@Unique
	private CubeRegionStorage dividedchunks$store;

	/** Copies of the sections outside the light range, which vanilla copies into its own data instead. */
	@Unique
	private Map<Integer, LevelChunkSection> dividedchunks$sections;

	@Inject(method = "copyOf", at = @At("RETURN"))
	private static void dividedchunks$snapshot(ServerLevel level, ChunkAccess chunk,
			CallbackInfoReturnable<SerializableChunkData> cir) {
		SerializableChunkDataMixin data = (SerializableChunkDataMixin) (Object) cir.getReturnValue();
		data.dividedchunks$store = CubeStores.get(level);
		LevelLightEngine lightEngine = level.getChunkSource().getLightEngine();
		int minLightSection = lightEngine.getMinLightSection();
		int maxLightSection = lightEngine.getMaxLightSection();
		Map<Integer, LevelChunkSection> sections = new HashMap<>();
		((ColumnSectionAccess) chunk).dividedchunks$forEachSection((sectionY, section) -> {
			if (sectionY < minLightSection || sectionY >= maxLightSection) {
				sections.put(sectionY, section.copy());
			}
		});
		data.dividedchunks$sections = sections;
	}

	@Inject(method = "write", at = @At("RETURN"))
	private void dividedchunks$writeColumn(CallbackInfoReturnable<CompoundTag> cir) {
		SerializableChunkData self = (SerializableChunkData) (Object) this;
		if (this.dividedchunks$store != null) {
			Map<Integer, LevelChunkSection> column = new HashMap<>(this.dividedchunks$sections);
			for (SerializableChunkData.SectionData data : self.sectionData()) {
				if (data.chunkSection() != null) {
					column.put(data.y(), data.chunkSection());
				}
			}
			ChunkPos pos = self.chunkPos();
			ColumnPersistence.write(this.dividedchunks$store, self.containerFactory(), pos.x(), pos.z(), column);
		}
		CompoundTag tag = cir.getReturnValue();
		tag.remove("sections");
		tag.remove("isLightOn");
	}

	@Inject(method = "read", at = @At("RETURN"))
	private void dividedchunks$readColumn(ServerLevel level, PoiManager poiManager, RegionStorageInfo regionInfo, ChunkPos pos,
			CallbackInfoReturnable<ProtoChunk> cir) {
		ProtoChunk chunk = cir.getReturnValue();
		ChunkAccess access = chunk instanceof ImposterProtoChunk imposter ? imposter.getWrapped() : chunk;
		ColumnPersistence.read(level, access);
	}
}
