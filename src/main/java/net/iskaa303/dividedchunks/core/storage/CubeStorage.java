package net.iskaa303.dividedchunks.core.storage;

import net.iskaa303.dividedchunks.api.util.CubePos;

import java.util.Optional;

/**
 * Sparse on-disk store of one opaque payload per {@link CubePos}; the store only compresses it.
 * Implementations must be safe to use from more than one thread.
 */
public interface CubeStorage extends AutoCloseable {

	/** The payload stored for {@code pos}, or empty if the cube is absent. */
	Optional<byte[]> get(CubePos pos);

	/** Stores {@code data} for {@code pos}, replacing any previous payload. */
	void put(CubePos pos, byte[] data);

	/** Removes {@code pos}, returning whether it was present. */
	boolean remove(CubePos pos);

	/** Writes pending changes to disk without closing. */
	void flush();

	@Override
	void close();
}
