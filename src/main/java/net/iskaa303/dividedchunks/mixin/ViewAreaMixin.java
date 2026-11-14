package net.iskaa303.dividedchunks.mixin;

import dev.kikugie.fletching_table.annotation.MixinIgnore;
import net.iskaa303.dividedchunks.core.client.BandRotation;
import net.iskaa303.dividedchunks.core.client.CubeRenderWindow;
//? if >=26.2 {
import net.minecraft.client.RotatingSectionStorage;
//?}
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Rotates the vertical band of the render grid so it follows the camera, instead of building a new grid,
 * which drops every compiled section. Below 26.2 the band is also injected into the level's section
 * bounds, which is where that grid reads its vertical extent from.
 */
@MixinIgnore
@Mixin(ViewArea.class)
public abstract class ViewAreaMixin {

	//? if <26.2 {
	/*@Shadow
	protected int sectionGridSizeY;

	@Shadow
	protected int sectionGridSizeX;

	@Shadow
	private int viewDistance;

	@Shadow
	public net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection[] sections;

	@Unique
	private int dividedchunks$windowMin;

	@Unique
	private static final boolean DEBUG = Boolean.getBoolean("dividedchunks.debugRender");

	@Inject(method = "setViewDistance", at = @At("RETURN"))
	private void dividedchunks$window(int renderDistance, CallbackInfo ci) {
		int min = CubeRenderWindow.min();
		if (min == Integer.MIN_VALUE) {
			return;
		}
		this.dividedchunks$windowMin = min;
		this.sectionGridSizeY = CubeRenderWindow.height();
	}

	@Inject(method = "repositionCamera", at = @At("HEAD"))
	private void dividedchunks$followBand(net.minecraft.core.SectionPos cameraSectionPos, CallbackInfo ci) {
		int height = this.sectionGridSizeY;
		// Room for the drawn cube at both ends, so the band moves in steps instead of every section.
		int margin = Math.max(this.viewDistance, height / 4);
		int sectionY = cameraSectionPos.y();
		if (this.dividedchunks$windowMin == Integer.MIN_VALUE
				|| (sectionY >= this.dividedchunks$windowMin + margin
						&& sectionY <= this.dividedchunks$windowMin + height - 1 - margin)) {
			return;
		}
		int newMin = sectionY - height / 2;
		BandRotation.rotate(this.sections, height, this.sectionGridSizeX, newMin - this.dividedchunks$windowMin);
		if (DEBUG) {
			net.iskaa303.dividedchunks.DividedChunks.LOGGER.info("Band rotated to sections {}..{}", newMin,
					newMin + height - 1);
		}
		this.dividedchunks$windowMin = newMin;
	}

	@Redirect(method = "createSections", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getMinSectionY()I"))
	private int dividedchunks$createSectionsMin(Level level) {
		return this.dividedchunks$windowMin;
	}

	@Redirect(method = "repositionCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getMinSectionY()I"))
	private int dividedchunks$repositionMin(Level level) {
		return this.dividedchunks$windowMin;
	}

	@Redirect(method = "getRenderSection(III)Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getMinSectionY()I"))
	private int dividedchunks$getRenderSectionMin(Level level) {
		return this.dividedchunks$windowMin;
	}

	@Redirect(method = "containsSection", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getMinSectionY()I"))
	private int dividedchunks$containsMin(Level level) {
		return this.dividedchunks$windowMin;
	}

	@Redirect(method = "containsSection", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getMaxSectionY()I"))
	private int dividedchunks$containsMax(Level level) {
		return this.dividedchunks$windowMin + this.sectionGridSizeY - 1;
	}
	*///?}

	//? if >=26.2 {
	@Shadow
	@Final
	private RotatingSectionStorage<?> sections;

	@Inject(method = "minY", at = @At("RETURN"), cancellable = true)
	private void dividedchunks$bandMinBlockY(CallbackInfoReturnable<Integer> cir) {
		cir.setReturnValue(this.sections.minY() << 4);
	}

	@Inject(method = "maxY", at = @At("RETURN"), cancellable = true)
	private void dividedchunks$bandMaxBlockY(CallbackInfoReturnable<Integer> cir) {
		cir.setReturnValue((this.sections.maxY() << 4) + 15);
	}
	//?}
}
