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

	/**
	 * Sky light of every block in the section at {@code sectionY}, indexed
	 * {@code localY << 8 | localZ << 4 | localX}, or {@code null} for a chunk that has no level.
	 */
	byte[] dividedchunks$skyLight(int sectionY);

	/**
	 * Block light of every block in the section at {@code sectionY}, indexed like the sky light, or
	 * {@code null} for a chunk that has no level.
	 */
	byte[] dividedchunks$blockLight(int sectionY);

	/** Drops every cached light a change at {@code blockY} can reach, here and in the neighbouring columns. */
	void dividedchunks$invalidateLightAround(int blockY);

	/** Drops the cached sky light at and below {@code blockY} in this column only. */
	void dividedchunks$invalidateSkyFrom(int blockY);

	/** Drops the cached block light of this column only. */
	void dividedchunks$invalidateBlockLight();

	/** Light-emitting blocks of this column packed as {@code x, y, z, level}. */
	int[] dividedchunks$emitters();

	/** Visits every stored section, lowest Y first. */
	void dividedchunks$forEachSection(BiConsumer<Integer, LevelChunkSection> consumer);
}
