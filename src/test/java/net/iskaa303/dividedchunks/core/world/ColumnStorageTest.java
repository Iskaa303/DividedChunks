package net.iskaa303.dividedchunks.core.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ColumnStorageTest {

	@Test
	void storesAndReadsBySectionY() {
		ColumnStorage<String> storage = new ColumnStorage<>();
		assertTrue(storage.isEmpty());

		assertNull(storage.put(-4, "bedrock"));
		assertEquals("bedrock", storage.get(-4));
		assertEquals("bedrock", storage.put(-4, "stone"));
		assertEquals("stone", storage.get(-4));
		assertEquals(1, storage.size());
	}

	@Test
	void absentSectionsReturnNull() {
		ColumnStorage<String> storage = new ColumnStorage<>();
		storage.put(5, "x");
		assertNull(storage.get(4));
		assertNull(storage.get(6));
		assertNull(storage.get(1_000_000));
		assertEquals("fallback", storage.getOrDefault(6, "fallback"));
		assertEquals("x", storage.getOrDefault(5, "fallback"));
	}

	@Test
	void iterationIsBottomToTop() {
		ColumnStorage<Integer> storage = new ColumnStorage<>();
		storage.put(10, 10);
		storage.put(-3, -3);
		storage.put(0, 0);
		storage.put(1_000_000, 1_000_000);

		List<Integer> visited = new ArrayList<>();
		storage.forEach((sectionY, value) -> visited.add(sectionY));
		assertEquals(List.of(-3, 0, 10, 1_000_000), visited);
	}

	@Test
	void boundsTrackTheOccupiedRange() {
		ColumnStorage<String> storage = new ColumnStorage<>();
		assertThrows(java.util.NoSuchElementException.class, storage::firstSectionY);
		assertThrows(java.util.NoSuchElementException.class, storage::lastSectionY);

		storage.put(3, "a");
		storage.put(-7, "b");
		storage.put(20, "c");
		assertEquals(-7, storage.firstSectionY());
		assertEquals(20, storage.lastSectionY());

		storage.remove(20);
		assertEquals(-7, storage.firstSectionY());
		assertEquals(3, storage.lastSectionY());
	}

	@Test
	void removeAndContains() {
		ColumnStorage<String> storage = new ColumnStorage<>();
		storage.put(2, "a");
		assertTrue(storage.containsKey(2));
		assertEquals("a", storage.remove(2));
		assertNull(storage.remove(2));
		assertFalse(storage.containsKey(2));
		assertTrue(storage.isEmpty());
	}

	@Test
	void rejectsNullValues() {
		ColumnStorage<String> storage = new ColumnStorage<>();
		assertThrows(NullPointerException.class, () -> storage.put(1, null));
	}

	@Test
	void clearEmptiesEverything() {
		ColumnStorage<String> storage = new ColumnStorage<>();
		storage.put(1, "a");
		storage.put(2, "b");
		storage.clear();
		assertTrue(storage.isEmpty());
		assertNull(storage.get(1));
	}
}
