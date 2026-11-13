package net.iskaa303.dividedchunks.core.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanillaChunksTest {

	@Test
	void eachSectionKeepsItsStatesAndBiomes() {
		CompoundTag chunk = chunk();

		Map<Integer, CompoundTag> sections = VanillaChunks.sections(chunk);

		assertEquals(1, sections.size());
		CompoundTag payload = sections.get(2);
		assertTrue(payload.contains("block_states"));
		assertTrue(payload.contains("biomes"));
		assertEquals(2, payload.size(), "a section payload carries nothing else");
	}

	@Test
	void aSectionWithoutBlockStatesIsSkipped() {
		CompoundTag chunk = chunk();
		CompoundTag lightOnly = new CompoundTag();
		lightOnly.putByte("Y", (byte) 3);
		chunk.getListOrEmpty("sections").add(lightOnly);

		assertFalse(VanillaChunks.sections(chunk).containsKey(3));
	}

	@Test
	void theColumnPayloadDropsSectionsAndTheLightFlag() {
		CompoundTag metadata = VanillaChunks.metadata(chunk());

		assertFalse(metadata.contains("sections"));
		assertFalse(metadata.contains("isLightOn"));
		assertEquals(1, metadata.getIntOr("DataVersion", -1));
	}

	private static CompoundTag chunk() {
		CompoundTag states = new CompoundTag();
		states.putBoolean("present", true);
		CompoundTag section = new CompoundTag();
		section.putByte("Y", (byte) 2);
		section.put("block_states", states);
		section.put("biomes", new CompoundTag());
		ListTag sections = new ListTag();
		sections.add(section);
		CompoundTag chunk = new CompoundTag();
		chunk.putInt("DataVersion", 1);
		chunk.putBoolean("isLightOn", true);
		chunk.put("sections", sections);
		return chunk;
	}
}
