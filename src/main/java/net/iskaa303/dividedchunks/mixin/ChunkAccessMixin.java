package net.iskaa303.dividedchunks.mixin;

import net.iskaa303.dividedchunks.DividedChunks;
import net.iskaa303.dividedchunks.core.light.ChunkBlocks;
import net.iskaa303.dividedchunks.core.light.SkyLight;
import net.iskaa303.dividedchunks.core.world.ColumnSectionAccess;
import net.iskaa303.dividedchunks.core.world.ColumnStorage;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Holds {@link ChunkAccess}'s sections in a sparse {@link ColumnStorage} keyed by section Y: the
 * constructor array seeds it, and {@code getSection}/{@code getSections} read it
 * ({@code getSections} returns a cached view in the same order). Sections outside the vanilla band
 * are reached through {@link LevelChunkMixin}.
 */
@Mixin(ChunkAccess.class)
public abstract class ChunkAccessMixin implements ColumnSectionAccess {

	@Shadow
	@Final
	protected LevelChunkSection[] sections;

	@Unique
	private final ColumnStorage<LevelChunkSection> dividedchunks$sections = new ColumnStorage<>();

	@Unique
	private LevelChunkSection[] dividedchunks$sectionView;

	@Unique
	private int[] dividedchunks$skyTops;

	/** How far above a section its light is followed; deeper columns fall back to the open-column rule. */
	@Unique
	private static final int MAX_SKY_CARRY_SECTIONS = 24;

	/** Section Y to its sky light; a concurrent map because the client renderer queries it too. */
	@Unique
	private final Map<Integer, byte[]> dividedchunks$skyLight = new ConcurrentHashMap<>();

	@Inject(method = "<init>", at = @At("RETURN"))
	private void dividedchunks$seedSections(CallbackInfo ci) {
		ChunkAccess self = (ChunkAccess) (Object) this;
		int minSectionY = self.getMinSectionY();
		int count = self.getSectionsCount();
		for (int index = 0; index < count; index++) {
			this.dividedchunks$sections.put(minSectionY + index, this.sections[index]);
		}
	}

	@Inject(method = "getSections", at = @At("HEAD"), cancellable = true)
	private void dividedchunks$getSections(CallbackInfoReturnable<LevelChunkSection[]> cir) {
		cir.setReturnValue(this.dividedchunks$sectionView());
	}

	@Inject(method = "getSection", at = @At("HEAD"), cancellable = true)
	private void dividedchunks$getSection(int sectionIndex, CallbackInfoReturnable<LevelChunkSection> cir) {
		ChunkAccess self = (ChunkAccess) (Object) this;
		cir.setReturnValue(this.dividedchunks$sections.get(self.getMinSectionY() + sectionIndex));
	}

	@Override
	public LevelChunkSection dividedchunks$getSectionByY(int sectionY) {
		return this.dividedchunks$sections.get(sectionY);
	}

	@Override
	public LevelChunkSection dividedchunks$putSection(int sectionY, LevelChunkSection section) {
		LevelChunkSection previous = this.dividedchunks$sections.put(sectionY, section);
		this.dividedchunks$sectionView = null;
		this.dividedchunks$invalidateSkyAround(sectionY << 4 | 15);
		this.dividedchunks$syncDense(sectionY, section);
		return previous;
	}

	@Override
	public LevelChunkSection dividedchunks$removeSection(int sectionY) {
		LevelChunkSection previous = this.dividedchunks$sections.remove(sectionY);
		this.dividedchunks$sectionView = null;
		this.dividedchunks$invalidateSkyAround(sectionY << 4 | 15);
		this.dividedchunks$syncDense(sectionY, null);
		return previous;
	}

	@Override
	public void dividedchunks$forEachSection(BiConsumer<Integer, LevelChunkSection> consumer) {
		this.dividedchunks$sections.forEach(consumer);
	}

	/** Vanilla still reads the band array directly in places, so it has to match the sparse store. */
	@Unique
	private void dividedchunks$syncDense(int sectionY, LevelChunkSection section) {
		int index = sectionY - ((ChunkAccess) (Object) this).getMinSectionY();
		if (index >= 0 && index < this.sections.length) {
			this.sections[index] = section;
		}
	}

	@Override
	public int dividedchunks$skyOccluderTop(int localX, int localZ) {
		int[] tops = this.dividedchunks$skyTops;
		if (tops == null) {
			tops = this.dividedchunks$skyTops = this.dividedchunks$computeSkyTops();
		}
		return tops[localZ << 4 | localX];
	}

	@Override
	public void dividedchunks$invalidateSkyFrom(int blockY) {
		int[] tops = this.dividedchunks$skyTops;
		if (tops != null) {
			int lowest = Integer.MAX_VALUE;
			for (int top : tops) {
				lowest = Math.min(lowest, top);
			}
			if (blockY >= lowest) {
				this.dividedchunks$skyTops = null;
			}
		}
		// Light never travels upwards, so only sections at or below the edit can change.
		int sectionY = SectionPos.blockToSectionCoord(blockY);
		this.dividedchunks$skyLight.keySet().removeIf(cached -> cached <= sectionY);
	}

	@Override
	public void dividedchunks$invalidateSkyAround(int blockY) {
		this.dividedchunks$invalidateSkyFrom(blockY);
		if (!((Object) this instanceof LevelChunk chunk)) {
			return;
		}
		Level level = chunk.getLevel();
		ChunkPos pos = chunk.getPos();
		int blocksPerLevel = SkyLight.blocksPerLevel(
				DividedChunks.config().forDimension(level.dimension().identifier().toString()).skyConeAngleDegrees);
		for (int dx = -blocksPerLevel; dx <= blocksPerLevel; dx++) {
			for (int dz = -blocksPerLevel; dz <= blocksPerLevel; dz++) {
				if (dx == 0 && dz == 0) {
					continue;
				}
				LevelChunk neighbour = level.getChunkSource().getChunkNow(pos.x() + dx, pos.z() + dz);
				if (neighbour != null) {
					((ColumnSectionAccess) neighbour).dividedchunks$invalidateSkyFrom(blockY);
				}
			}
		}
	}

	@Override
	public byte[] dividedchunks$skyLight(int sectionY) {
		return this.dividedchunks$skyLight(sectionY, 0);
	}

	@Unique
	private byte[] dividedchunks$skyLight(int sectionY, int depth) {
		if (!((Object) this instanceof LevelChunk chunk)) {
			return null;
		}
		byte[] light = this.dividedchunks$skyLight.get(sectionY);
		if (light != null) {
			return light;
		}
		Level level = chunk.getLevel();
		int blocksPerLevel = SkyLight.blocksPerLevel(
				DividedChunks.config().forDimension(level.dimension().identifier().toString()).skyConeAngleDegrees);
		light = SkyLight.compute(chunk.getPos().getMinBlockX(), chunk.getPos().getMinBlockZ(), sectionY << 4,
				blocksPerLevel,
				(worldX, worldZ) -> this.dividedchunks$skyOccluderTop(level, worldX, worldZ),
				new ChunkBlocks(level), this.dividedchunks$skyLightAbove(level, sectionY + 1, depth));
		this.dividedchunks$skyLight.put(sectionY, light);
		return light;
	}

	/**
	 * Floor row of the section above, lit first, or {@code null} when nothing stored sits above this one:
	 * an absent section is air, which the open-column rule already accounts for.
	 */
	@Unique
	private byte[] dividedchunks$skyLightAbove(Level level, int sectionY, int depth) {
		if (depth >= MAX_SKY_CARRY_SECTIONS || this.dividedchunks$sections.get(sectionY) == null) {
			return null;
		}
		byte[] above = this.dividedchunks$skyLight(sectionY, depth + 1);
		return above == null ? null : Arrays.copyOf(above, 256);
	}

	/** A column of an unloaded chunk stores no blocks, so it cannot occlude. */
	@Unique
	private int dividedchunks$skyOccluderTop(Level level, int worldX, int worldZ) {
		LevelChunk chunk = level.getChunkSource()
				.getChunkNow(SectionPos.blockToSectionCoord(worldX), SectionPos.blockToSectionCoord(worldZ));
		return chunk == null
				? Integer.MIN_VALUE
				: ((ColumnSectionAccess) chunk).dividedchunks$skyOccluderTop(worldX & 15, worldZ & 15);
	}

	/** Scans the highest sections first, so the first damping block found per column is its top. */
	@Unique
	private int[] dividedchunks$computeSkyTops() {
		int[] tops = new int[256];
		Arrays.fill(tops, Integer.MIN_VALUE);
		List<Integer> sectionYs = new ArrayList<>(this.dividedchunks$sections.size());
		this.dividedchunks$sections.forEach((sectionY, section) -> sectionYs.add(sectionY));
		for (int i = sectionYs.size() - 1; i >= 0; i--) {
			int sectionY = sectionYs.get(i);
			LevelChunkSection section = this.dividedchunks$sections.get(sectionY);
			if (section == null || section.hasOnlyAir()) {
				continue;
			}
			int baseY = sectionY << 4;
			for (int localZ = 0; localZ < 16; localZ++) {
				for (int localX = 0; localX < 16; localX++) {
					int index = localZ << 4 | localX;
					if (tops[index] != Integer.MIN_VALUE) {
						continue;
					}
					for (int localY = 15; localY >= 0; localY--) {
						if (section.getBlockState(localX, localY, localZ).getLightDampening() > 0) {
							tops[index] = baseY + localY;
							break;
						}
					}
				}
			}
		}
		return tops;
	}

	@Unique
	private LevelChunkSection[] dividedchunks$sectionView() {
		LevelChunkSection[] view = this.dividedchunks$sectionView;
		if (view == null) {
			ChunkAccess self = (ChunkAccess) (Object) this;
			int minSectionY = self.getMinSectionY();
			view = new LevelChunkSection[self.getSectionsCount()];
			for (int index = 0; index < view.length; index++) {
				view[index] = this.dividedchunks$sections.get(minSectionY + index);
			}
			this.dividedchunks$sectionView = view;
		}
		return view;
	}
}
