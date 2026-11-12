package net.iskaa303.dividedchunks.core.world;

import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Mth;

/**
 * Checks a column's entity payload. A mob that walks into the next column leaves a copy behind in the
 * column it came from, because that column is only rewritten when it is next saved; loading the copy
 * spawns a second entity with the same UUID, which vanilla drops with a warning and which can put the
 * stale copy in charge of a live mob.
 */
public final class EntityTags {

	private EntityTags() {
	}

	/** Removes the entities of {@code tag} that are not inside the column {@code chunkX, chunkZ}. */
	public static void dropForeignEntities(CompoundTag tag, int chunkX, int chunkZ) {
		ListTag entities = tag.getListOrEmpty("Entities");
		if (!entities.isEmpty()) {
			entities.removeIf(entity -> entity instanceof CompoundTag compound
					&& !inColumn(compound, chunkX, chunkZ));
		}
	}

	/** Entities without a usable position are kept: there is nothing to judge them by. */
	private static boolean inColumn(CompoundTag entity, int chunkX, int chunkZ) {
		ListTag pos = entity.getListOrEmpty("Pos");
		if (pos.size() < 3) {
			return true;
		}
		Double x = pos.getDouble(0).orElse(null);
		Double z = pos.getDouble(2).orElse(null);
		if (x == null || z == null) {
			return true;
		}
		return SectionPos.blockToSectionCoord(Mth.floor(x)) == chunkX
				&& SectionPos.blockToSectionCoord(Mth.floor(z)) == chunkZ;
	}
}
