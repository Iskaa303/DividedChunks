package net.iskaa303.dividedchunks.mixin;

import net.iskaa303.dividedchunks.core.world.ColumnPersistence;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Writes every section of a chunk column to the {@code .dcr} store and strips it from the stored
 * chunk tag, so the metadata payload carries no blocks.
 * shortcut: sections are encoded on the save thread, so a column with many sections lengthens that tick.
 */
@Mixin(SerializableChunkData.class)
public abstract class SerializableChunkDataMixin {

	@Inject(method = "copyOf", at = @At("RETURN"))
	private static void dividedchunks$writeColumn(ServerLevel level, ChunkAccess chunk,
			CallbackInfoReturnable<SerializableChunkData> cir) {
		ColumnPersistence.write(level, chunk);
	}

	/** Removes the sections with their light, which now live in {@code .dcr}; the light flag would stop vanilla relighting. */
	@Inject(method = "write", at = @At("RETURN"))
	private void dividedchunks$stripSections(CallbackInfoReturnable<CompoundTag> cir) {
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
