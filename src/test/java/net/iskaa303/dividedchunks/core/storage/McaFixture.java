package net.iskaa303.dividedchunks.core.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.Deflater;

/** Writes a small vanilla region file for tests: header, then one zlib chunk per entry. */
public final class McaFixture {
	private static final int HEADER_BYTES = 8192;
	private static final int SECTOR_BYTES = 4096;

	private McaFixture() {
	}

	public static void write(Path file, List<Entry> entries) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(new byte[HEADER_BYTES]);
		int sector = 2;
		ByteBuffer header = ByteBuffer.allocate(HEADER_BYTES);
		for (Entry entry : entries) {
			byte[] payload = compress(entry.tag());
			int length = payload.length + 1;
			ByteBuffer record = ByteBuffer.allocate(4 + length);
			record.putInt(length);
			record.put((byte) 2);
			record.put(payload);
			int index = (entry.chunkX() & 31) + (entry.chunkZ() & 31) * 32;
			int used = (record.capacity() + SECTOR_BYTES - 1) / SECTOR_BYTES;
			header.putInt(index * 4, sector << 8 | used);
			out.write(record.array());
			out.write(new byte[used * SECTOR_BYTES - record.capacity()]);
			sector += used;
		}
		byte[] body = out.toByteArray();
		System.arraycopy(header.array(), 0, body, 0, HEADER_BYTES);
		Files.createDirectories(file.getParent());
		Files.write(file, body);
	}

	/**
	 * Adds an entry whose payload lives in a {@code c.<x>.<z>.mcc} file next to the region file, which is
	 * what vanilla does for a chunk too large for the region file's own sectors.
	 */
	public static void writeExternal(Path file, int chunkX, int chunkZ, CompoundTag tag) throws IOException {
		byte[] body = Files.readAllBytes(file);
		int sector = (body.length + SECTOR_BYTES - 1) / SECTOR_BYTES;
		ByteBuffer grown = ByteBuffer.allocate(sector * SECTOR_BYTES + SECTOR_BYTES);
		grown.put(body);
		grown.putInt(sector * SECTOR_BYTES, 1);
		grown.put(sector * SECTOR_BYTES + 4, (byte) (0x80 | 2));
		int index = (chunkX & 31) + (chunkZ & 31) * 32;
		grown.putInt(index * 4, sector << 8 | 1);
		Files.write(file, grown.array());
		Files.write(file.resolveSibling("c." + chunkX + "." + chunkZ + ".mcc"), compress(tag));
	}

	private static byte[] compress(CompoundTag tag) throws IOException {
		ByteArrayOutputStream raw = new ByteArrayOutputStream();
		try (DataOutputStream data = new DataOutputStream(raw)) {
			NbtIo.write(tag, data);
		}
		Deflater deflater = new Deflater();
		deflater.setInput(raw.toByteArray());
		deflater.finish();
		ByteArrayOutputStream compressed = new ByteArrayOutputStream();
		byte[] buffer = new byte[4096];
		while (!deflater.finished()) {
			compressed.write(buffer, 0, deflater.deflate(buffer));
		}
		deflater.end();
		return compressed.toByteArray();
	}

	/** One chunk of a region file. */
	public record Entry(int chunkX, int chunkZ, CompoundTag tag) {
	}
}
