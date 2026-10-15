package net.iskaa303.dividedchunks.core.world;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;

import java.util.Objects;
import java.util.function.BiConsumer;

/** Sparse per-column store of one value per section Y; iteration runs lowest to highest. */
public final class ColumnStorage<T> {

	private final Int2ObjectSortedMap<T> bySectionY = new Int2ObjectAVLTreeMap<>();

	/** Stores {@code value} at {@code sectionY}, returning the previous value or {@code null}. */
	public T put(int sectionY, T value) {
		return this.bySectionY.put(sectionY, Objects.requireNonNull(value, "value"));
	}

	/** The stored value, or {@code null} if the section is absent. */
	public T get(int sectionY) {
		return this.bySectionY.get(sectionY);
	}

	/** The stored value, or {@code fallback} if the section is absent. */
	public T getOrDefault(int sectionY, T fallback) {
		T value = this.bySectionY.get(sectionY);
		return value != null ? value : fallback;
	}

	/** Removes and returns the value at {@code sectionY}, or {@code null} if absent. */
	public T remove(int sectionY) {
		return this.bySectionY.remove(sectionY);
	}

	public boolean containsKey(int sectionY) {
		return this.bySectionY.containsKey(sectionY);
	}

	public int size() {
		return this.bySectionY.size();
	}

	public boolean isEmpty() {
		return this.bySectionY.isEmpty();
	}

	/** Lowest occupied section Y. Throws if empty. */
	public int firstSectionY() {
		return this.bySectionY.firstIntKey();
	}

	/** Highest occupied section Y. Throws if empty. */
	public int lastSectionY() {
		return this.bySectionY.lastIntKey();
	}

	/** Visits every stored section from the lowest Y to the highest. */
	public void forEach(BiConsumer<Integer, T> action) {
		for (Int2ObjectMap.Entry<T> entry : this.bySectionY.int2ObjectEntrySet()) {
			action.accept(entry.getIntKey(), entry.getValue());
		}
	}

	public void clear() {
		this.bySectionY.clear();
	}
}
