package net.iskaa303.dividedchunks.mixin;

import net.iskaa303.dividedchunks.DividedChunks;
import net.iskaa303.dividedchunks.core.light.BlockLight;
import net.iskaa303.dividedchunks.core.light.ChunkBlocks;
import net.iskaa303.dividedchunks.core.light.Emitters;
import net.iskaa303.dividedchunks.core.light.LightCache;
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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

	@Unique
	private final LightCache dividedchunks$skyLight = new LightCache();

	@Unique
	private final LightCache dividedchunks$blockLight = new LightCache();

	/** Emitters of this column, or {@code null} until scanned. */
	@Unique
	private int[] dividedchunks$emitters;

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
		this.dividedchunks$invalidateLightAround(sectionY << 4 | 15);
		this.dividedchunks$syncDense(sectionY, section);
		return previous;
	}

	@Override
	public LevelChunkSection dividedchunks$removeSection(int sectionY) {
		if (this.dividedchunks$isInBand(sectionY)) {
			// Vanilla reads a band section without a null check, so the band always keeps its own.
			return null;
		}
		LevelChunkSection previous = this.dividedchunks$sections.remove(sectionY);
		this.dividedchunks$sectionView = null;
		this.dividedchunks$invalidateLightAround(sectionY << 4 | 15);
		return previous;
	}

	@Override
	public void dividedchunks$forEachSection(BiConsumer<Integer, LevelChunkSection> consumer) {
		this.dividedchunks$sections.forEach(consumer);
	}

	/** True for a section Y inside the vanilla band, which vanilla stores and syncs itself. */
	@Unique
	private boolean dividedchunks$isInBand(int sectionY) {
		ChunkAccess self = (ChunkAccess) (Object) this;
		return sectionY >= self.getMinSectionY() && sectionY <= self.getMaxSectionY();
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
	public void dividedchunks$setSkyOccluderTops(int[] tops) {
		// The server sees every stored section of the column, the client only the cubes around it.
		this.dividedchunks$skyTops = tops;
		this.dividedchunks$skyLight.clear();
	}

	@Override
	public void dividedchunks$invalidateLightAround(int blockY) {
		this.dividedchunks$invalidateSkyFrom(blockY);
		this.dividedchunks$emitters = null;
		this.dividedchunks$blockLight.clear();
		// Block light reaches at most 15 blocks, so its neighbours are the 8 around this column.
		this.dividedchunks$forEachNeighbour(1, ColumnSectionAccess::dividedchunks$invalidateBlockLight);
		if (!((Object) this instanceof LevelChunk chunk)) {
			return;
		}
		int blocksPerLevel = SkyLight.blocksPerLevel(DividedChunks.config()
				.forDimension(chunk.getLevel().dimension().identifier().toString()).skyConeAngleDegrees);
		this.dividedchunks$forEachNeighbour(blocksPerLevel,
				neighbour -> neighbour.dividedchunks$invalidateSkyFrom(blockY));
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
		this.dividedchunks$skyLight.clearFrom(SectionPos.blockToSectionCoord(blockY));
	}

	@Override
	public void dividedchunks$invalidateBlockLight() {
		this.dividedchunks$blockLight.clear();
	}

	@Override
	public byte[] dividedchunks$skyLight(int sectionY) {
		if (!((Object) this instanceof LevelChunk chunk)) {
			return null;
		}
		Level level = chunk.getLevel();
		int reach = SkyLight.blocksPerLevel(
				DividedChunks.config().forDimension(level.dimension().identifier().toString()).skyConeAngleDegrees);
		// Light only flows down, so starting at the highest occluder and walking every section below it
		// lights the whole column in one pass; sections in between are air and pass light on.
		byte[] above = null;
		for (int y = Math.max(sectionY, this.dividedchunks$highestOccluderSection()); y >= sectionY; y--) {
			byte[] previous = above;
			above = this.dividedchunks$skyLight.get(y, requested -> SkyLight.compute(
					chunk.getPos().getMinBlockX(), chunk.getPos().getMinBlockZ(), requested << 4, reach,
					(worldX, worldZ) -> this.dividedchunks$skyOccluderTop(level, worldX, worldZ),
					new ChunkBlocks(level), previous));
		}
		return above;
	}

	/** Section holding the column's highest occluding block, or {@link Integer#MIN_VALUE} without one. */
	@Unique
	private int dividedchunks$highestOccluderSection() {
		int[] tops = this.dividedchunks$skyTops;
		if (tops == null) {
			tops = this.dividedchunks$skyTops = this.dividedchunks$computeSkyTops();
		}
		int top = Integer.MIN_VALUE;
		for (int value : tops) {
			top = Math.max(top, value);
		}
		return top == Integer.MIN_VALUE ? top : SectionPos.blockToSectionCoord(top);
	}

	@Override
	public byte[] dividedchunks$blockLight(int sectionY) {
		if (!((Object) this instanceof LevelChunk chunk)) {
			return null;
		}
		return this.dividedchunks$blockLight.get(sectionY, requested -> {
			Level level = chunk.getLevel();
			return BlockLight.compute(chunk.getPos().getMinBlockX(), requested << 4,
					chunk.getPos().getMinBlockZ(), Emitters.near(level, (ChunkAccess) (Object) this, requested,
							BlockLight.MAX),
					new ChunkBlocks(level));
		});
	}

	@Override
	public int[] dividedchunks$emitters() {
		int[] emitters = this.dividedchunks$emitters;
		if (emitters == null) {
			emitters = this.dividedchunks$emitters = (Object) this instanceof LevelChunk chunk
					? Emitters.of(chunk.getLevel(), (ChunkAccess) (Object) this)
					: new int[0];
		}
		return emitters;
	}

	/** Visits the loaded columns around this one out to {@code radius} chunks. */
	@Unique
	private void dividedchunks$forEachNeighbour(int radius, Consumer<ColumnSectionAccess> action) {
		if (!((Object) this instanceof LevelChunk chunk)) {
			return;
		}
		Level level = chunk.getLevel();
		ChunkPos pos = chunk.getPos();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (dx == 0 && dz == 0) {
					continue;
				}
				LevelChunk neighbour = level.getChunkSource().getChunkNow(pos.x() + dx, pos.z() + dz);
				if (neighbour != null) {
					action.accept((ColumnSectionAccess) neighbour);
				}
			}
		}
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
