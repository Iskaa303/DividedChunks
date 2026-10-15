package net.iskaa303.dividedchunks.core.world;

import com.mojang.serialization.Codec;
import net.iskaa303.dividedchunks.core.storage.StorageException;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.PalettedContainerRO;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Encodes a {@link LevelChunkSection} to bytes and back as name-based NBT, so stored cubes survive
 * registry changes between sessions.
 */
public final class SectionCodec {

	private SectionCodec() {
	}

	public static byte[] encode(LevelChunkSection section, PalettedContainerFactory factory) {
		CompoundTag tag = new CompoundTag();
		tag.store("block_states", factory.blockStatesContainerCodec(), section.getStates());
		tag.store("biomes", factory.biomeContainerCodec(), section.getBiomes());
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (DataOutputStream out = new DataOutputStream(bytes)) {
			NbtIo.write(tag, out);
		} catch (IOException e) {
			throw new StorageException("Failed to encode a section", e);
		}
		return bytes.toByteArray();
	}

	public static LevelChunkSection decode(byte[] data, PalettedContainerFactory factory) {
		try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
			CompoundTag tag = NbtIo.read(in, NbtAccounter.unlimitedHeap());
			Codec<PalettedContainer<BlockState>> statesCodec = factory.blockStatesContainerCodec();
			Codec<PalettedContainerRO<Holder<Biome>>> biomesCodec = factory.biomeContainerCodec();
			PalettedContainer<BlockState> states = tag.getCompound("block_states")
					.map(container -> statesCodec.parse(NbtOps.INSTANCE, container).getOrThrow())
					.orElseGet(factory::createForBlockStates);
			PalettedContainerRO<Holder<Biome>> biomes = tag.getCompound("biomes")
					.map(container -> biomesCodec.parse(NbtOps.INSTANCE, container).getOrThrow())
					.orElseGet(factory::createForBiomes);
			return new LevelChunkSection(states, biomes);
		} catch (IOException e) {
			throw new StorageException("Failed to decode a stored section", e);
		}
	}
}
