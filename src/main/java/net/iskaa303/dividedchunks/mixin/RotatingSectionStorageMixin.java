package net.iskaa303.dividedchunks.mixin;

import dev.kikugie.fletching_table.annotation.MixinIgnore;
//? if >=26.2 {
import net.iskaa303.dividedchunks.DividedChunks;
import net.iskaa303.dividedchunks.core.client.BandRotation;
import net.minecraft.client.RotatingSectionStorage;
//?}
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets the vertical band follow the camera. Vanilla rotates only the horizontal axes, so its Y range is
 * fixed and any change to it means building a new grid, which drops every compiled section. Here the
 * rows are rotated in place instead: a section that stays in the band keeps its mesh, and only the ones
 * that wrap out are re-pointed and rebuilt, exactly like a column that wraps horizontally.
 */
//? if >=26.2 {
@MixinIgnore
@Mixin(RotatingSectionStorage.class)
public abstract class RotatingSectionStorageMixin {

	/** Traces band moves, which the eye sees as nothing at all when the rotation works. */
	private static final boolean DEBUG = Boolean.getBoolean("dividedchunks.debugRender");

	@Shadow
	@Final
	private RotatingSectionStorage.Node<?>[] nodes;

	@Shadow
	@Final
	private int sectionGridSizeY;

	@Shadow
	@Final
	private int sectionGridSizeXZ;

	@Shadow
	@Mutable
	private int minY;

	@Shadow
	@Mutable
	private int maxY;

	@Shadow
	@Final
	private int radius;

	@Inject(method = "repositionCenter", at = @At("HEAD"))
	private void dividedchunks$followVertically(SectionPos center, CallbackInfoReturnable<Boolean> cir) {
		int height = this.sectionGridSizeY;
		// Room for the drawn cube at both ends, so the band moves in steps instead of every section.
		int margin = Math.max(this.radius, height / 4);
		int sectionY = center.y();
		if (sectionY >= this.minY + margin && sectionY <= this.maxY - margin) {
			return;
		}
		int newMinY = sectionY - height / 2;
		if (DEBUG) {
			DividedChunks.LOGGER.info("Band rotated to sections {}..{}", newMinY, newMinY + height - 1);
		}
		dividedchunks$rotateTo(newMinY);
	}

	/** Moves the band to {@code newMinY}, taking every value that stays in it along with its mesh. */
	@Unique
	private void dividedchunks$rotateTo(int newMinY) {
		int delta = newMinY - this.minY;
		BandRotation.rotate(this.nodes, this.sectionGridSizeY, this.sectionGridSizeXZ, delta);
		this.minY = newMinY;
		this.maxY = newMinY + this.sectionGridSizeY - 1;
	}
}
//?} else {
/*@MixinIgnore
@Mixin(targets = "net.minecraft.client.renderer.LevelRenderer")
public abstract class RotatingSectionStorageMixin {
}
*///?}
