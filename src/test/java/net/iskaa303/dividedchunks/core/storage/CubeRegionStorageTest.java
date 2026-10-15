package net.iskaa303.dividedchunks.core.storage;

import net.iskaa303.dividedchunks.api.util.CubePos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CubeRegionStorageTest {

	@TempDir
	Path directory;

	private static byte[] bytes(int... values) {
		byte[] out = new byte[values.length];
		for (int i = 0; i < values.length; i++) {
			out[i] = (byte) values[i];
		}
		return out;
	}

	@Test
	void routesCubesIntoRegionFiles() {
		try (CubeRegionStorage storage = new CubeRegionStorage(this.directory)) {
			storage.put(new CubePos(0, 0, 0), bytes(1));
			storage.put(new CubePos(31, 0, 31), bytes(2));
			storage.put(new CubePos(32, 0, 32), bytes(3));
			storage.put(new CubePos(-1, 0, -1), bytes(4));

			assertTrue(Files.exists(this.directory.resolve("r.0.0.dcr")));
			assertTrue(Files.exists(this.directory.resolve("r.1.1.dcr")));
			assertTrue(Files.exists(this.directory.resolve("r.-1.-1.dcr")));

			assertArrayEquals(bytes(1), storage.get(new CubePos(0, 0, 0)).orElseThrow());
			assertArrayEquals(bytes(2), storage.get(new CubePos(31, 0, 31)).orElseThrow());
			assertArrayEquals(bytes(3), storage.get(new CubePos(32, 0, 32)).orElseThrow());
			assertArrayEquals(bytes(4), storage.get(new CubePos(-1, 0, -1)).orElseThrow());
		}
	}

	@Test
	void persistsAcrossReopen() {
		try (CubeRegionStorage storage = new CubeRegionStorage(this.directory)) {
			storage.put(new CubePos(100, -500, 100), bytes(1, 2, 3));
			storage.flush();
		}
		try (CubeRegionStorage storage = new CubeRegionStorage(this.directory)) {
			assertArrayEquals(bytes(1, 2, 3), storage.get(new CubePos(100, -500, 100)).orElseThrow());
		}
	}

	@Test
	void getDoesNotCreateRegionFile() {
		try (CubeRegionStorage storage = new CubeRegionStorage(this.directory)) {
			assertTrue(storage.get(new CubePos(100, 5, 100)).isEmpty());
			assertTrue(storage.get(new CubePos(100, 5, 100)).isEmpty());
		}
		assertFalse(Files.exists(this.directory.resolve("r.3.3.dcr")));
	}

	@Test
	void removesAcrossRegions() {
		try (CubeRegionStorage storage = new CubeRegionStorage(this.directory)) {
			CubePos pos = new CubePos(-40, 10, -40);
			storage.put(pos, bytes(1));
			assertTrue(storage.remove(pos));
			assertFalse(storage.remove(pos));
			assertTrue(storage.get(pos).isEmpty());
		}
	}

	@Test
	void readsAWholeColumn() {
		try (CubeRegionStorage storage = new CubeRegionStorage(this.directory)) {
			storage.put(new CubePos(5, 0, 6), bytes(0));
			storage.put(new CubePos(5, 1_000_000, 6), bytes(1));
			storage.put(new CubePos(5, -1_000_000, 6), bytes(2));
			storage.put(new CubePos(6, 0, 6), bytes(3));

			Map<Integer, byte[]> column = storage.readColumn(5, 6);
			assertEquals(3, column.size());
			assertArrayEquals(bytes(0), column.get(0));
			assertArrayEquals(bytes(1), column.get(1_000_000));
			assertArrayEquals(bytes(2), column.get(-1_000_000));
		}
	}

	@Test
	void readColumnOfMissingRegionIsEmpty() {
		try (CubeRegionStorage storage = new CubeRegionStorage(this.directory)) {
			assertTrue(storage.readColumn(5, 6).isEmpty());
		}
		assertFalse(Files.exists(this.directory.resolve("r.0.0.dcr")));
	}

	@Test
	void worksWithSmallRegionCache() {
		try (CubeRegionStorage storage = new CubeRegionStorage(this.directory, 1)) {
			for (int i = 0; i < 6; i++) {
				storage.put(new CubePos(i * 32, i, i * 32), bytes(i));
			}
			for (int i = 0; i < 6; i++) {
				assertArrayEquals(bytes(i), storage.get(new CubePos(i * 32, i, i * 32)).orElseThrow());
			}
		}
	}
}
