package net.iskaa303.dividedchunks.core.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * Reads a vanilla region file ({@code .mca}): an 8 KiB header of 1024 sector entries, then per chunk a
 * length, a compression id and the NBT payload. A header entry packs sector number {@code << 8 | count}.
 */
public final class McaFile {

	private static final int HEADER_BYTES = 8192;
	private static final int SECTOR_BYTES = 4096;
	private static final int SECTOR_MASK = 0xFFFFFF;
	private static final int CHUNKS_PER_AXIS = 32;
	private static final byte EXTERNAL_FLAG = (byte) 0x80;

	private McaFile() {
	}

	/** One chunk of a region file, in chunk coordinates. */
	public record Chunk(int x, int z, CompoundTag tag) {
	}

	/** Reads every stored chunk, in header order. */
	public static void read(Path file, Sink sink) {
		byte[] data;
		try {
			data = Files.readAllBytes(file);
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to read " + file, e);
		}
		if (data.length < HEADER_BYTES) {
			return;
		}
		ByteBuffer buffer = ByteBuffer.wrap(data);
		String[] name = file.getFileName().toString().split("\\.");
		int regionX = number(name, 1);
		int regionZ = number(name, 2);
		for (int index = 0; index < CHUNKS_PER_AXIS * CHUNKS_PER_AXIS; index++) {
			int packed = buffer.getInt(index * 4);
			int sector = (packed >>> 8) & SECTOR_MASK;
			if (sector == 0) {
				continue;
			}
			int start = sector * SECTOR_BYTES;
			if (start + 5 > data.length) {
				continue;
			}
			int length = buffer.getInt(start);
			int chunkX = regionX * CHUNKS_PER_AXIS + index % CHUNKS_PER_AXIS;
			int chunkZ = regionZ * CHUNKS_PER_AXIS + index / CHUNKS_PER_AXIS;
			CompoundTag tag;
			if ((data[start + 4] & EXTERNAL_FLAG) != 0) {
				tag = parseExternal(file, chunkX, chunkZ, (byte) (data[start + 4] & ~EXTERNAL_FLAG));
			} else {
				long end = (long) start + 4 + length;
				if (length < 1 || end > data.length) {
					continue;
				}
				tag = parse(data, start + 5, length - 1, data[start + 4]);
			}
			if (tag != null) {
				sink.accept(new Chunk(chunkX, chunkZ, tag));
			}
		}
	}

	/** A chunk too large for its region file lives next to it as {@code c.<x>.<z>.mcc}. */
	private static CompoundTag parseExternal(Path region, int chunkX, int chunkZ, byte compression) {
		Path file = region.getParent().resolve("c." + chunkX + "." + chunkZ + ".mcc");
		try {
			byte[] data = Files.readAllBytes(file);
			return parse(data, 0, data.length, compression);
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to read " + file, e);
		}
	}

	@FunctionalInterface
	public interface Sink {
		void accept(Chunk chunk);
	}

	/** Region coordinate from {@code r.<x>.<z>.mca}; {@code axis} 1 is x, 2 is z. */
	private static int number(String[] name, int axis) {
		if (name.length < axis + 1) {
			return 0;
		}
		try {
			return Integer.parseInt(name[axis]);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static CompoundTag parse(byte[] data, int offset, int length, byte compression) {
		if (length <= 0) {
			return null;
		}
		try (InputStream raw = new ByteArrayInputStream(data, offset, length)) {
			InputStream inflated = switch (compression) {
				case 1 -> new GZIPInputStream(raw);
				case 2 -> new InflaterInputStream(raw);
				case 3 -> raw;
				default -> null;
			};
			if (inflated == null) {
				return null;
			}
			try (DataInputStream in = new DataInputStream(inflated)) {
				return NbtIo.read(in, NbtAccounter.unlimitedHeap());
			}
		} catch (EOFException e) {
			return null;
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to decode a chunk", e);
		}
	}
}
