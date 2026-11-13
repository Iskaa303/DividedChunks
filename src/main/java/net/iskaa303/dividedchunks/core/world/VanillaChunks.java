package net.iskaa303.dividedchunks.core.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.Map;

/** Turns a vanilla chunk tag into the payloads a cube region file keeps. */
public final class VanillaChunks {

	private VanillaChunks() {
	}

	/** Section Y to a payload holding only {@code block_states} and {@code biomes}. */
	public static Map<Integer, CompoundTag> sections(CompoundTag chunk) {
		Map<Integer, CompoundTag> sections = new HashMap<>();
		for (Tag tag : chunk.getListOrEmpty("sections")) {
			if (!(tag instanceof CompoundTag section)) {
				continue;
			}
			CompoundTag states = section.getCompoundOrEmpty("block_states");
			if (states.isEmpty()) {
				continue;
			}
			CompoundTag payload = new CompoundTag();
			payload.put("block_states", states);
			section.getCompound("biomes").ifPresent(biomes -> payload.put("biomes", biomes));
			sections.put((int) section.getByteOr("Y", (byte) 0), payload);
		}
		return sections;
	}

	/** The chunk tag without the sections and light flag the store keeps separately. */
	public static CompoundTag metadata(CompoundTag chunk) {
		CompoundTag tag = chunk.copy();
		tag.remove("sections");
		tag.remove("isLightOn");
		return tag;
	}
}
