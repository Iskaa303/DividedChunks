package net.iskaa303.dividedchunks.core.world;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkTagsTest {

	@Test
	void anIntArraySurvivesTheRoundTrip() {
		int[] tops = new int[256];
		Arrays.fill(tops, Integer.MIN_VALUE);
		tops[0] = 341;
		tops[17] = -64;
		CompoundTag tag = new CompoundTag();
		tag.putIntArray("tops", tops);

		int[] read = ChunkTags.fromBytes(ChunkTags.toBytes(tag)).getIntArray("tops").orElseThrow();

		assertEquals(256, read.length);
		assertArrayEquals(tops, read);
	}

	@Test
	void anAbsentArrayStaysAbsent() {
		CompoundTag tag = ChunkTags.fromBytes(ChunkTags.toBytes(new CompoundTag()));

		assertTrue(tag.getIntArray("tops").isEmpty());
	}
}
