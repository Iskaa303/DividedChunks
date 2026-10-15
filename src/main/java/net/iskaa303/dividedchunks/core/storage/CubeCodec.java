package net.iskaa303.dividedchunks.core.storage;

import java.io.ByteArrayOutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Compression of one cube payload. The id is written into every record so a later version can add a
 * codec and still read existing files.
 */
enum CubeCodec {

	ZLIB(1);

	private final byte id;

	CubeCodec(int id) {
		this.id = (byte) id;
	}

	byte id() {
		return this.id;
	}

	static CubeCodec byId(byte id) {
		for (CubeCodec codec : values()) {
			if (codec.id == id) {
				return codec;
			}
		}
		throw new StorageException("Unknown cube codec id " + id);
	}

	byte[] compress(byte[] data) {
		Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION);
		try {
			deflater.setInput(data);
			deflater.finish();
			ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, data.length / 2));
			byte[] buffer = new byte[8192];
			while (!deflater.finished()) {
				out.write(buffer, 0, deflater.deflate(buffer));
			}
			return out.toByteArray();
		} finally {
			deflater.end();
		}
	}

	byte[] decompress(byte[] data) {
		Inflater inflater = new Inflater();
		try {
			inflater.setInput(data);
			ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, data.length * 2));
			byte[] buffer = new byte[8192];
			while (!inflater.finished()) {
				int written = inflater.inflate(buffer);
				if (written == 0) {
					break;
				}
				out.write(buffer, 0, written);
			}
			return out.toByteArray();
		} catch (DataFormatException e) {
			throw new StorageException("Corrupt cube payload", e);
		} finally {
			inflater.end();
		}
	}
}
