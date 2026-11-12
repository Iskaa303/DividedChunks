package net.iskaa303.dividedchunks.core.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntityTagsTest {

	@Test
	void keepsOnlyTheEntitiesOfTheColumn() {
		CompoundTag tag = payload(
				entity("inside", 100.5, 8.0),
				entity("nextColumnEast", 132.0, 64.0),
				entity("nextColumnNegative", -0.5, 64.0));
		EntityTags.dropForeignEntities(tag, 6, 0);
		assertEquals(List.of("inside"), ids(tag));
	}

	@Test
	void keepsCoordinatesOnTheColumnBorder() {
		CompoundTag tag = payload(
				entity("westEdge", 96.0, 0.0),
				entity("eastEdge", 111.999, 15.0));
		EntityTags.dropForeignEntities(tag, 6, 0);
		assertEquals(List.of("westEdge", "eastEdge"), ids(tag));
	}

	@Test
	void keepsEntitiesWithoutAPosition() {
		CompoundTag entity = new CompoundTag();
		entity.putString("id", "minecraft:marker");
		CompoundTag tag = payload(entity);
		EntityTags.dropForeignEntities(tag, 6, 0);
		assertEquals(List.of("minecraft:marker"), ids(tag));
	}

	@Test
	void anEmptyPayloadStaysEmpty() {
		CompoundTag tag = new CompoundTag();
		EntityTags.dropForeignEntities(tag, 0, 0);
		assertEquals(List.of(), ids(tag));
	}

	private static List<String> ids(CompoundTag tag) {
		return tag.getListOrEmpty("Entities").stream()
				.map(entity -> ((CompoundTag) entity).getStringOr("id", "?"))
				.toList();
	}

	private static CompoundTag payload(CompoundTag... entities) {
		ListTag list = new ListTag();
		for (CompoundTag entity : entities) {
			list.add(entity);
		}
		CompoundTag tag = new CompoundTag();
		tag.put("Entities", list);
		return tag;
	}

	private static CompoundTag entity(String id, double x, double z) {
		ListTag pos = new ListTag();
		pos.add(DoubleTag.valueOf(x));
		pos.add(DoubleTag.valueOf(64.0));
		pos.add(DoubleTag.valueOf(z));
		CompoundTag entity = new CompoundTag();
		entity.putString("id", id);
		entity.put("Pos", pos);
		return entity;
	}
}
