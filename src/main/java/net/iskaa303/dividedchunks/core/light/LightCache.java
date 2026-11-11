package net.iskaa303.dividedchunks.core.light;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntFunction;

/** The light arrays of one column, keyed by section Y; concurrent because the render thread reads it. */
public final class LightCache {

	private final Map<Integer, byte[]> entries = new ConcurrentHashMap<>();

	public byte[] get(int sectionY, IntFunction<byte[]> compute) {
		byte[] light = this.entries.get(sectionY);
		if (light == null) {
			light = compute.apply(sectionY);
			this.entries.put(sectionY, light);
		}
		return light;
	}

	public void clear() {
		this.entries.clear();
	}

	/** Drops the sections at and below {@code sectionY}. */
	public void clearFrom(int sectionY) {
		this.entries.keySet().removeIf(cached -> cached <= sectionY);
	}
}
