package net.iskaa303.dividedchunks.core.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Uncompressed NBT bytes of a chunk's metadata tag; the store compresses the payload itself. */
public final class ChunkTags {

	private ChunkTags() {
	}

	public static byte[] toBytes(CompoundTag tag) {
		ByteArrayOutputStream out = new ByteArrayOutputStream(512);
		try (DataOutputStream dataOutput = new DataOutputStream(out)) {
			NbtIo.write(tag, dataOutput);
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to encode chunk tag", e);
		}
		return out.toByteArray();
	}

	public static CompoundTag fromBytes(byte[] data) {
		try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(data))) {
			return NbtIo.read(input, NbtAccounter.unlimitedHeap());
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to decode chunk tag", e);
		}
	}
}
