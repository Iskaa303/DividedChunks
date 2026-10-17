package net.iskaa303.dividedchunks.mixin;

import dev.kikugie.fletching_table.annotation.MixinIgnore;
import net.iskaa303.dividedchunks.core.world.ColumnSectionAccess;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets a render section read a section outside the vanilla band from the sparse column storage.
 * Targeted by name because SectionCopy is package-private on some versions.
 */
@MixinIgnore
@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionCopy")
public abstract class SectionCopyMixin {

	@Shadow
	@Final
	@Mutable
	private PalettedContainer<BlockState> section;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void dividedchunks$sparseSection(LevelChunk chunk, int sectionIndex, CallbackInfo ci) {
		if (sectionIndex >= 0 && sectionIndex < chunk.getSectionsCount()) {
			return;
		}
		LevelChunkSection stored = ((ColumnSectionAccess) chunk)
				.dividedchunks$getSectionByY(chunk.getMinSectionY() + sectionIndex);
		this.section = stored == null || stored.hasOnlyAir() ? null : stored.getStates().copy();
	}
}
