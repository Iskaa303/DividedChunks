package net.iskaa303.dividedchunks.core.storage;

/** Thrown when a {@link CubeStorage} operation fails. */
public final class StorageException extends RuntimeException {

	public StorageException(String message) {
		super(message);
	}

	public StorageException(String message, Throwable cause) {
		super(message, cause);
	}
}
