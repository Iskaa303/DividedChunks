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
		return previous;
	}

	@Override
	public LevelChunkSection dividedchunks$removeSection(int sectionY) {
		LevelChunkSection previous = this.dividedchunks$sections.remove(sectionY);
		this.dividedchunks$sectionView = null;
		return previous;
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
