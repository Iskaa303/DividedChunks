package net.iskaa303.dividedchunks.core.storage;

import net.iskaa303.dividedchunks.api.util.CubePos;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.CRC32;

/**
 * One cube region file: a small header followed by a sequence of compressed cube records.
 *
 * <p>Records are append-only, so overwriting or removing a cube leaves dead space behind. The
 * in-memory index maps each cube to its payload offset and is rebuilt on open by scanning record
 * headers; bytes left by an interrupted write are dropped. Once dead space passes the compaction
 * threshold the file is rewritten with only the live records.</p>
 *
 * <p>Layout is big-endian:
 * <pre>
 *   file:   magic "DCRF" (int), format version (short)
 *   record: chunkX (int), sectionY (int), chunkZ (int), payload length (int),
 *           codec id (byte), CRC32 of the payload (int), payload
 * </pre></p>
 */
final class CubeRegionFile implements AutoCloseable {

	/** Chunk columns per region along each axis. */
	static final int REGION_SIZE = 32;

	private static final int MAGIC = 0x44435246;
	private static final short FORMAT_VERSION = 1;
	private static final int FILE_HEADER_SIZE = 6;
	private static final int RECORD_HEADER_SIZE = 21;
	private static final long DEFAULT_COMPACTION_THRESHOLD = 1L << 20;

	private final Path path;
	private final int regionX;
	private final int regionZ;
	private final long compactionThreshold;
	private final Map<CubePos, Long> index = new HashMap<>();
	private RandomAccessFile file;
	private long liveBytes;

	CubeRegionFile(Path path, int regionX, int regionZ) {
		this(path, regionX, regionZ, DEFAULT_COMPACTION_THRESHOLD);
	}

	CubeRegionFile(Path path, int regionX, int regionZ, long compactionThreshold) {
		this.path = path;
		this.regionX = regionX;
		this.regionZ = regionZ;
		this.compactionThreshold = compactionThreshold;
		try {
			Path parent = path.toAbsolutePath().getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
			this.file = new RandomAccessFile(path.toFile(), "rw");
			if (this.file.length() == 0) {
				this.file.writeInt(MAGIC);
				this.file.writeShort(FORMAT_VERSION);
			} else {
				readFileHeader();
				rebuildIndex();
			}
		} catch (IOException e) {
			closeQuietly();
			throw new StorageException("Failed to open cube region " + path, e);
		}
	}

	synchronized Optional<byte[]> get(CubePos pos) {
		Long offset = this.index.get(pos);
		if (offset == null) {
			return Optional.empty();
		}
		try {
			this.file.seek(offset - RECORD_HEADER_SIZE);
			this.file.readInt();
			this.file.readInt();
			this.file.readInt();
			int length = this.file.readInt();
			byte codecId = this.file.readByte();
			int expectedCrc = this.file.readInt();
			byte[] payload = new byte[length];
			this.file.readFully(payload);
			if (crc32(payload) != expectedCrc) {
				throw new StorageException("Corrupt cube payload at " + pos);
			}
			return Optional.of(CubeCodec.byId(codecId).decompress(payload));
		} catch (IOException e) {
			throw new StorageException("Failed to read cube " + pos, e);
		}
	}

	synchronized void put(CubePos pos, byte[] data) {
		Objects.requireNonNull(data, "data");
		checkRegion(pos);
		try {
			byte[] payload = CubeCodec.ZLIB.compress(data);
			Long previous = this.index.get(pos);
			if (previous != null) {
				this.liveBytes -= recordSize(previous);
			}
			long offset = this.file.length();
			this.file.seek(offset);
			writeRecordHeader(pos.x(), pos.y(), pos.z(), payload.length, CubeCodec.ZLIB.id(), crc32(payload));
			this.file.write(payload);
			this.index.put(pos, offset + RECORD_HEADER_SIZE);
			this.liveBytes += RECORD_HEADER_SIZE + payload.length;
			maybeCompact();
		} catch (IOException e) {
			throw new StorageException("Failed to write cube " + pos, e);
		}
	}

	synchronized boolean remove(CubePos pos) {
		Long previous = this.index.remove(pos);
		if (previous == null) {
			return false;
		}
		try {
			this.liveBytes -= recordSize(previous);
			maybeCompact();
			return true;
		} catch (IOException e) {
			throw new StorageException("Failed to remove cube " + pos, e);
		}
	}

	synchronized int count() {
		return this.index.size();
	}

	synchronized void flush() {
		try {
			this.file.getFD().sync();
		} catch (IOException e) {
			throw new StorageException("Failed to flush cube region " + this.path, e);
		}
	}

	@Override
	public synchronized void close() {
		try {
			this.file.close();
		} catch (IOException e) {
			throw new StorageException("Failed to close cube region " + this.path, e);
		}
	}

	private void readFileHeader() throws IOException {
		this.file.seek(0);
		int magic = this.file.readInt();
		short version = this.file.readShort();
		if (magic != MAGIC) {
			throw new StorageException("Not a DividedChunks cube region: " + this.path);
		}
		if (version != FORMAT_VERSION) {
			throw new StorageException("Unsupported cube region version " + version + " in " + this.path);
		}
	}

	private void rebuildIndex() throws IOException {
		this.index.clear();
		this.liveBytes = 0;
		long length = this.file.length();
		long position = FILE_HEADER_SIZE;
		while (position + RECORD_HEADER_SIZE <= length) {
			this.file.seek(position);
			int chunkX = this.file.readInt();
			int sectionY = this.file.readInt();
			int chunkZ = this.file.readInt();
			int payloadLength = this.file.readInt();
			this.file.readByte();
			int crc = this.file.readInt();
			long payloadOffset = position + RECORD_HEADER_SIZE;
			long end = payloadOffset + payloadLength;
			if (payloadLength < 0 || end > length || !contains(chunkX, chunkZ)) {
				break;
			}
			// Validating the payload here reads every record on open, but it is what lets a crash
			// mid-write be recovered by dropping the bad record instead of handing it to a caller.
			byte[] payload = new byte[payloadLength];
			this.file.readFully(payload);
			if (crc32(payload) != crc) {
				break;
			}
			this.index.put(new CubePos(chunkX, sectionY, chunkZ), payloadOffset);
			this.liveBytes += RECORD_HEADER_SIZE + payloadLength;
			position = end;
		}
		if (position < length) {
			this.file.setLength(position);
		}
	}

	private void maybeCompact() throws IOException {
		long dead = this.file.length() - FILE_HEADER_SIZE - this.liveBytes;
		if (dead > 0 && dead >= this.compactionThreshold) {
			compact();
		}
	}

	private void compact() throws IOException {
		Path temp = this.path.resolveSibling(this.path.getFileName() + ".tmp");
		Map<CubePos, Long> compacted = new HashMap<>();
		long live = 0;
		try (RandomAccessFile out = new RandomAccessFile(temp.toFile(), "rw")) {
			out.setLength(0);
			out.writeInt(MAGIC);
			out.writeShort(FORMAT_VERSION);
			for (Map.Entry<CubePos, Long> entry : this.index.entrySet()) {
				this.file.seek(entry.getValue() - RECORD_HEADER_SIZE);
				int chunkX = this.file.readInt();
				int sectionY = this.file.readInt();
				int chunkZ = this.file.readInt();
				int length = this.file.readInt();
				byte codecId = this.file.readByte();
				int crc = this.file.readInt();
				byte[] payload = new byte[length];
				this.file.readFully(payload);
				long offset = out.getFilePointer();
				out.writeInt(chunkX);
				out.writeInt(sectionY);
				out.writeInt(chunkZ);
				out.writeInt(length);
				out.writeByte(codecId);
				out.writeInt(crc);
				out.write(payload);
				compacted.put(entry.getKey(), offset + RECORD_HEADER_SIZE);
				live += RECORD_HEADER_SIZE + length;
			}
			out.getFD().sync();
		}
		this.file.close();
		Files.move(temp, this.path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		this.file = new RandomAccessFile(this.path.toFile(), "rw");
		this.index.clear();
		this.index.putAll(compacted);
		this.liveBytes = live;
	}

	private long recordSize(long payloadOffset) throws IOException {
		this.file.seek(payloadOffset - RECORD_HEADER_SIZE + 12);
		return RECORD_HEADER_SIZE + this.file.readInt();
	}

	private void writeRecordHeader(int chunkX, int sectionY, int chunkZ, int length, byte codecId, int crc)
			throws IOException {
		this.file.writeInt(chunkX);
		this.file.writeInt(sectionY);
		this.file.writeInt(chunkZ);
		this.file.writeInt(length);
		this.file.writeByte(codecId);
		this.file.writeInt(crc);
	}

	private void checkRegion(CubePos pos) {
		if (!contains(pos.x(), pos.z())) {
			throw new IllegalArgumentException("Cube " + pos + " is outside region " + this.regionX + "," + this.regionZ);
		}
	}

	private boolean contains(int chunkX, int chunkZ) {
		long minX = (long) this.regionX * REGION_SIZE;
		long minZ = (long) this.regionZ * REGION_SIZE;
		return chunkX >= minX && chunkX < minX + REGION_SIZE
				&& chunkZ >= minZ && chunkZ < minZ + REGION_SIZE;
	}

	private void closeQuietly() {
		try {
			if (this.file != null) {
				this.file.close();
			}
		} catch (IOException ignored) {
			// Nothing useful can be done while failing to open.
		}
	}

	private static int crc32(byte[] data) {
		CRC32 crc = new CRC32();
		crc.update(data);
		return (int) crc.getValue();
	}
}
