package net.iskaa303.dividedchunks.core.world;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/** What a world directory holds: cube region files, vanilla {@code .mca} files, or neither. */
public enum WorldKind {

	/** Only {@code .dcr} files, the one kind this mod can open. */
	CUBIC,

	/** Vanilla chunk files, which have to go through {@link WorldConverter} first. */
	VANILLA,

	/** Neither. */
	EMPTY;

	public static WorldKind of(Path worldDirectory) {
		if (!Files.isDirectory(worldDirectory)) {
			return EMPTY;
		}
		if (hasFile(worldDirectory, ".dcr")) {
			return CUBIC;
		}
		if (hasFile(worldDirectory, ".mca")) {
			return VANILLA;
		}
		return EMPTY;
	}

	private static boolean hasFile(Path worldDirectory, String suffix) {
		try (Stream<Path> files = Files.walk(worldDirectory, 6)) {
			return files.anyMatch(path -> path.getFileName().toString().endsWith(suffix));
		} catch (IOException e) {
			return false;
		}
	}
}
