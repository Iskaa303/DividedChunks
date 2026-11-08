package net.iskaa303.dividedchunks.mixin;

import net.iskaa303.dividedchunks.core.world.ColumnSectionAccess;
import net.iskaa303.dividedchunks.core.world.ColumnStorage;
import net.minecraft.world.level.chunk.ChunkAccess;
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
		this.dividedchunks$skyTops = null;
		return previous;
	}

	@Override
	public LevelChunkSection dividedchunks$removeSection(int sectionY) {
		LevelChunkSection previous = this.dividedchunks$sections.remove(sectionY);
		this.dividedchunks$sectionView = null;
		this.dividedchunks$skyTops = null;
		return previous;
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
	public void dividedchunks$invalidateSkyOccluders() {
		this.dividedchunks$skyTops = null;
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
