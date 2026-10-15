package net.iskaa303.dividedchunks.core.world;

import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Sparse section access added to {@code ChunkAccess} by its mixin, so other mixins can reach the
 * column storage without the dense-array index round-trip.
 */
public interface ColumnSectionAccess {

	/** The section stored at {@code sectionY}, or {@code null}. */
	LevelChunkSection dividedchunks$getSectionByY(int sectionY);

	/** Stores {@code section} at {@code sectionY}, returning the previous section or {@code null}. */
	LevelChunkSection dividedchunks$putSection(int sectionY, LevelChunkSection section);
}
