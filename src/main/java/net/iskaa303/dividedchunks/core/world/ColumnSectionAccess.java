package net.iskaa303.dividedchunks.core.world;

import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.function.BiConsumer;

/** Sparse section access that {@code ChunkAccessMixin} adds to {@code ChunkAccess}. */
public interface ColumnSectionAccess {

	/** The section stored at {@code sectionY}, or {@code null}. */
	LevelChunkSection dividedchunks$getSectionByY(int sectionY);

	/** Stores {@code section} at {@code sectionY}, returning the previous section or {@code null}. */
	LevelChunkSection dividedchunks$putSection(int sectionY, LevelChunkSection section);

	/** Removes the section at {@code sectionY}, returning the previous section or {@code null}. */
	LevelChunkSection dividedchunks$removeSection(int sectionY);

	/** Highest Y of a sky-damping block in the column at {@code localX}/{@code localZ}, or {@link Integer#MIN_VALUE}. */
	int dividedchunks$skyOccluderTop(int localX, int localZ);

	/** Drops the cached sky occluder heights. */
	void dividedchunks$invalidateSkyOccluders();

	/** Visits every stored section, lowest Y first. */
	void dividedchunks$forEachSection(BiConsumer<Integer, LevelChunkSection> consumer);
}
