package net.iskaa303.dividedchunks.mixin;

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
 * Holds {@link ChunkAccess}'s sections in a sparse {@link ColumnStorage} keyed by section Y.
 *
 * <p>The constructor's dense array seeds the storage, {@code getSection(int)} and
 * {@code getSections()} read it, and {@code getSections()} returns a cached view holding the
 * same sections in the same order.</p>
 */
@Mixin(ChunkAccess.class)
public abstract class ChunkAccessMixin {

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
