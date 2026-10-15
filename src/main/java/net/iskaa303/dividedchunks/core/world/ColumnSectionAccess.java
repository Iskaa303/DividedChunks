package net.iskaa303.dividedchunks.core.world;

import net.minecraft.world.level.chunk.LevelChunkSection;

/** Sparse section access that {@code ChunkAccessMixin} adds to {@code ChunkAccess}. */
public interface ColumnSectionAccess {

	/** The section stored at {@code sectionY}, or {@code null}. */
	LevelChunkSection dividedchunks$getSectionByY(int sectionY);

	/** Stores {@code section} at {@code sectionY}, returning the previous section or {@code null}. */
	LevelChunkSection dividedchunks$putSection(int sectionY, LevelChunkSection section);

	/** Removes the section at {@code sectionY}, returning the previous section or {@code null}. */
	LevelChunkSection dividedchunks$removeSection(int sectionY);
}
