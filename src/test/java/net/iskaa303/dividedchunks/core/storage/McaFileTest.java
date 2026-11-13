package net.iskaa303.dividedchunks.core.storage;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McaFileTest {

	@TempDir
	Path folder;

	@Test
	void readsTheChunksOfARegion() throws IOException {
		Path file = this.folder.resolve("r.0.0.mca");
		McaFixture.write(file, List.of(new McaFixture.Entry(0, 0, chunkTag(1)), new McaFixture.Entry(3, 5, chunkTag(2))));

		List<String> read = new ArrayList<>();
		McaFile.read(file, chunk -> read.add(chunk.x() + "," + chunk.z() + "=" + chunk.tag().getIntOr("DataVersion", -1)));

		assertEquals(List.of("0,0=1", "3,5=2"), read);
	}

	@Test
	void regionCoordinatesComeFromTheFileName() throws IOException {
		Path file = this.folder.resolve("r.-7.42.mca");
		McaFixture.write(file, List.of(new McaFixture.Entry(1, 0, chunkTag(1))));

		List<McaFile.Chunk> read = new ArrayList<>();
		McaFile.read(file, read::add);

		assertEquals(-7 * 32 + 1, read.getFirst().x());
		assertEquals(42 * 32, read.getFirst().z());
	}

	@Test
	void aHeaderOfOnlyZerosReadsNoChunks() throws IOException {
		Path file = this.folder.resolve("r.0.0.mca");
		Files.write(file, new byte[8192]);
		List<McaFile.Chunk> read = new ArrayList<>();
		McaFile.read(file, read::add);
		assertTrue(read.isEmpty());
	}

	@Test
	void aChunkTooLargeForTheRegionFileIsReadFromItsOwnFile() throws IOException {
		Path file = this.folder.resolve("r.0.0.mca");
		McaFixture.write(file, List.of(new McaFixture.Entry(0, 0, chunkTag(1))));
		McaFixture.writeExternal(file, 4, 2, chunkTag(9));

		List<McaFile.Chunk> read = new ArrayList<>();
		McaFile.read(file, read::add);

		assertEquals(2, read.size());
		assertEquals(4, read.getLast().x());
		assertEquals(2, read.getLast().z());
		assertEquals(9, read.getLast().tag().getIntOr("DataVersion", -1));
	}

	private static CompoundTag chunkTag(int version) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("DataVersion", version);
		return tag;
	}
}
