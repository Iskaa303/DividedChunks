package net.iskaa303.dividedchunks.core.world;

import net.iskaa303.dividedchunks.api.util.CubePos;
import net.iskaa303.dividedchunks.core.storage.CubeRegionStorage;
import net.iskaa303.dividedchunks.core.storage.McaFixture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldConverterTest {


	private Path world() {
		return this.root.resolve("world");
	}

	private Path original() {
		return this.root.resolve("world_non_cubic");
	}

	@TempDir
	Path root;

	@Test
	void aVanillaWorldBecomesACubicWorld() throws IOException {
		McaFixture.write(this.world().resolve("dimensions/minecraft/overworld/region/r.0.0.mca"), List.of(new McaFixture.Entry(0, 0, chunk(0, "minecraft:stone"))));
		McaFixture.write(this.world().resolve("dimensions/minecraft/overworld/entities/r.0.0.mca"), List.of(new McaFixture.Entry(0, 0, entities())));
		McaFixture.write(this.world().resolve("dimensions/minecraft/overworld/poi/r.0.0.mca"), List.of(new McaFixture.Entry(0, 0, poi())));
		McaFixture.write(this.world().resolve("dimensions/minecraft/the_nether/region/r.0.0.mca"),
				List.of(new McaFixture.Entry(0, 0, chunk(0, "minecraft:netherrack"))));

		WorldConverter.Report report = WorldConverter.convert(this.world(), WorldConverter.Progress.NONE);

		assertEquals(new WorldConverter.Report(2, 2, 2, 1, 1), report);
		assertEquals(WorldKind.CUBIC, WorldKind.of(this.world()));
		try (CubeRegionStorage overworld = new CubeRegionStorage(this.world().resolve("dimensions/minecraft/overworld/dividedchunks"))) {
			assertTrue(overworld.getTag(CubeRegionStorage.CHUNK_TAG_Y, 0, 0).isPresent(), "column payload");
			assertTrue(overworld.getTag(CubeRegionStorage.ENTITY_TAG_Y, 0, 0).isPresent(), "entity payload");
			assertTrue(overworld.getTag(CubeRegionStorage.POI_TAG_Y, 0, 0).isPresent(), "poi payload");
			CompoundTag section = ChunkTags.fromBytes(overworld.get(new CubePos(0, 0, 0)).orElseThrow());
			assertTrue(section.contains("block_states"), "section payload keeps its block states");
			assertTrue(section.contains("biomes"), "section payload keeps its biomes");
		}
		try (CubeRegionStorage nether = new CubeRegionStorage(this.world().resolve("dimensions/minecraft/the_nether/dividedchunks"))) {
			assertTrue(nether.get(new CubePos(0, 0, 0)).isPresent(), "nether section");
		}
	}

	@Test
	void anAlreadyCubicWorldIsNotConvertedAgain() throws IOException {
		McaFixture.write(this.world().resolve("dimensions/minecraft/overworld/region/r.0.0.mca"), List.of(new McaFixture.Entry(0, 0, chunk(0, "minecraft:stone"))));
		WorldConverter.convert(this.world(), WorldConverter.Progress.NONE);

		WorldConverter.Report again = WorldConverter.convert(this.world(), WorldConverter.Progress.NONE);

		assertEquals(new WorldConverter.Report(0, 0, 0, 0, 0), again);
	}

	@Test
	void theVanillaWorldIsKeptAndTheCopyHasNoVanillaFiles() throws IOException {
		McaFixture.write(this.world().resolve("dimensions/minecraft/overworld/region/r.0.0.mca"), List.of(new McaFixture.Entry(0, 0, chunk(0, "minecraft:stone"))));
		Files.writeString(this.world().resolve("level.dat"), "level");

		WorldConverter.convert(this.world(), WorldConverter.Progress.NONE);

		assertTrue(Files.exists(this.original().resolve("dimensions/minecraft/overworld/region/r.0.0.mca")), "original kept");
		assertFalse(Files.exists(this.world().resolve("dimensions/minecraft/overworld/region")), "no region copy");
		assertEquals("level", Files.readString(this.world().resolve("level.dat")));
		assertFalse(Files.exists(this.root.resolve("world.converting")));
	}

	@Test
	void anEmptySectionIsNotStored() throws IOException {
		CompoundTag chunk = chunk(0, "minecraft:stone");
		CompoundTag air = new CompoundTag();
		air.putByte("Y", (byte) 5);
		chunk.getListOrEmpty("sections").add(air);
		McaFixture.write(this.world().resolve("dimensions/minecraft/overworld/region/r.0.0.mca"), List.of(new McaFixture.Entry(0, 0, chunk)));

		WorldConverter.convert(this.world(), WorldConverter.Progress.NONE);

		try (CubeRegionStorage store = new CubeRegionStorage(this.world().resolve("dimensions/minecraft/overworld/dividedchunks"))) {
			Optional<byte[]> section = store.get(new CubePos(0, 5, 0));
			assertTrue(section.isEmpty(), "a section without block states is dropped");
		}
	}

	private static CompoundTag chunk(int sectionY, String block) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("DataVersion", 1);
		tag.put("biomes", biomeTag());
		CompoundTag section = new CompoundTag();
		section.putByte("Y", (byte) sectionY);
		CompoundTag states = new CompoundTag();
		states.put("palette", palette(block));
		section.put("block_states", states);
		section.put("biomes", biomeTag());
		ListTag sections = new ListTag();
		sections.add(section);
		tag.put("sections", sections);
		tag.putBoolean("isLightOn", true);
		return tag;
	}

	private static CompoundTag biomeTag() {
		CompoundTag biomes = new CompoundTag();
		biomes.put("palette", palette("minecraft:plains"));
		return biomes;
	}

	private static ListTag palette(String name) {
		CompoundTag entry = new CompoundTag();
		entry.putString("Name", name);
		ListTag palette = new ListTag();
		palette.add(entry);
		return palette;
	}

	private static CompoundTag entities() {
		CompoundTag tag = new CompoundTag();
		ListTag list = new ListTag();
		CompoundTag entity = new CompoundTag();
		entity.putString("id", "minecraft:pig");
		list.add(entity);
		tag.put("Entities", list);
		return tag;
	}

	private static CompoundTag poi() {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("Valid", true);
		return tag;
	}
}
