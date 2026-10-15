package net.iskaa303.dividedchunks.core.storage;

import net.iskaa303.dividedchunks.api.util.CubePos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CubeRegionFileTest {

	@TempDir
	Path directory;

	private Path file() {
		return this.directory.resolve("r.0.0.dcr");
	}

	private static byte[] bytes(int... values) {
		byte[] out = new byte[values.length];
		for (int i = 0; i < values.length; i++) {
			out[i] = (byte) values[i];
		}
		return out;
	}

	@Test
	void storesAndReadsBack() {
		try (CubeRegionFile region = new CubeRegionFile(this.file(), 0, 0)) {
			region.put(new CubePos(1, 2, 3), bytes(1, 2, 3));
			assertArrayEquals(bytes(1, 2, 3), region.get(new CubePos(1, 2, 3)).orElseThrow());
			assertTrue(region.get(new CubePos(1, 2, 4)).isEmpty());
			assertEquals(1, region.count());
		}
	}

	@Test
	void overwritesExistingCube() {
		try (CubeRegionFile region = new CubeRegionFile(this.file(), 0, 0)) {
			CubePos pos = new CubePos(4, -2_000_000, 5);
			region.put(pos, bytes(1));
			region.put(pos, bytes(9, 8));
			assertArrayEquals(bytes(9, 8), region.get(pos).orElseThrow());
			assertEquals(1, region.count());
		}
	}

	@Test
	void removesCube() {
		try (CubeRegionFile region = new CubeRegionFile(this.file(), 0, 0)) {
			CubePos pos = new CubePos(7, 8, 9);
			region.put(pos, bytes(1));
			assertTrue(region.remove(pos));
			assertFalse(region.remove(pos));
			assertTrue(region.get(pos).isEmpty());
			assertEquals(0, region.count());
		}
	}

	@Test
	void reopensExistingRegion() {
		Path file = this.file();
		try (CubeRegionFile region = new CubeRegionFile(file, 0, 0)) {
			region.put(new CubePos(2, 3, 4), bytes(5, 6));
			region.put(new CubePos(2, 4, 4), bytes(7));
			region.flush();
		}
		try (CubeRegionFile region = new CubeRegionFile(file, 0, 0)) {
			assertArrayEquals(bytes(5, 6), region.get(new CubePos(2, 3, 4)).orElseThrow());
			assertArrayEquals(bytes(7), region.get(new CubePos(2, 4, 4)).orElseThrow());
			assertEquals(2, region.count());
		}
	}

	@Test
	void roundTripsLargePayload() {
		byte[] data = new byte[64 * 1024];
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte) (i % 7);
		}
		try (CubeRegionFile region = new CubeRegionFile(this.file(), 0, 0)) {
			region.put(new CubePos(0, 1_000_000, 0), data);
			assertArrayEquals(data, region.get(new CubePos(0, 1_000_000, 0)).orElseThrow());
		}
	}

	@Test
	void handlesExtremeCoordinates() {
		int regionX = Math.floorDiv(Integer.MIN_VALUE, CubeRegionFile.REGION_SIZE);
		int regionZ = Math.floorDiv(Integer.MAX_VALUE, CubeRegionFile.REGION_SIZE);
		CubePos low = new CubePos(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
		CubePos high = new CubePos(
				Integer.MIN_VALUE + CubeRegionFile.REGION_SIZE - 1,
				Integer.MAX_VALUE,
				Integer.MAX_VALUE - CubeRegionFile.REGION_SIZE + 1);
		try (CubeRegionFile region = new CubeRegionFile(this.directory.resolve("r.extreme.dcr"), regionX, regionZ)) {
			region.put(low, bytes(1));
			region.put(high, bytes(2));
			assertArrayEquals(bytes(1), region.get(low).orElseThrow());
			assertArrayEquals(bytes(2), region.get(high).orElseThrow());
		}
	}

	@Test
	void rejectsCubeOutsideRegion() {
		try (CubeRegionFile region = new CubeRegionFile(this.file(), 0, 0)) {
			assertThrows(IllegalArgumentException.class, () -> region.put(new CubePos(-1, 0, 0), bytes(1)));
			assertThrows(IllegalArgumentException.class, () -> region.put(new CubePos(0, 0, 32), bytes(1)));
		}
	}

	@Test
	void rejectsForeignFile() throws IOException {
		Path file = this.file();
		Files.write(file, bytes(1, 2, 3, 4, 5, 6));
		assertThrows(StorageException.class, () -> new CubeRegionFile(file, 0, 0));
	}

	@Test
	void dropsPartialTrailingRecord() throws IOException {
		Path file = this.file();
		try (CubeRegionFile region = new CubeRegionFile(file, 0, 0)) {
			region.put(new CubePos(1, 1, 1), bytes(42));
		}
		long validSize = Files.size(file);
		ByteBuffer partial = ByteBuffer.allocate(21);
		partial.putInt(2).putInt(2).putInt(2).putInt(1000).put((byte) 1).putInt(0);
		Files.write(file, partial.array(), StandardOpenOption.APPEND);
		try (CubeRegionFile region = new CubeRegionFile(file, 0, 0)) {
			assertArrayEquals(bytes(42), region.get(new CubePos(1, 1, 1)).orElseThrow());
			assertTrue(region.get(new CubePos(2, 2, 2)).isEmpty());
		}
		assertEquals(validSize, Files.size(file));
	}

	@Test
	void detectsCorruptPayload() throws IOException {
		Path file = this.file();
		try (CubeRegionFile region = new CubeRegionFile(file, 0, 0)) {
			region.put(new CubePos(1, 1, 1), bytes(1, 2, 3, 4, 5, 6, 7, 8));
		}
		try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw")) {
			raf.seek(6 + 21);
			int first = raf.read();
			raf.seek(6 + 21);
			raf.write(first ^ 0xFF);
		}
		// The corrupt record is dropped on open, so the region reads as empty.
		try (CubeRegionFile region = new CubeRegionFile(file, 0, 0)) {
			assertTrue(region.get(new CubePos(1, 1, 1)).isEmpty());
			assertEquals(0, region.count());
		}
	}

	@Test
	void compactsWhenDeadSpaceGrows() {
		Path file = this.file();
		try (CubeRegionFile region = new CubeRegionFile(file, 0, 0, 0)) {
			CubePos pos = new CubePos(1, 1, 1);
			for (int i = 0; i < 20; i++) {
				region.put(pos, bytes(i, i, i, i));
			}
			assertEquals(1, region.count());
			assertArrayEquals(bytes(19, 19, 19, 19), region.get(pos).orElseThrow());
		}
		try (CubeRegionFile region = new CubeRegionFile(file, 0, 0)) {
			assertArrayEquals(bytes(19, 19, 19, 19), region.get(new CubePos(1, 1, 1)).orElseThrow());
		}
	}
}
