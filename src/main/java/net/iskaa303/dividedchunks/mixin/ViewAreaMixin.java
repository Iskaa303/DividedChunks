package net.iskaa303.dividedchunks.mixin;

import dev.kikugie.fletching_table.annotation.MixinIgnore;
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
 * Gives the dense ViewArea a windowed vertical band: built as a constructor argument from 26.2, and
 * redirected into the grid by hand below it. From 26.2 the band also reports its current block bounds,
 * which move with the camera because the band rotates.
 */
@MixinIgnore
@Mixin(ViewArea.class)
public abstract class ViewAreaMixin {

	//? if <26.2 {
	/*@Shadow
	protected int sectionGridSizeY;

	@Unique
	private int dividedchunks$windowMin;

	@Inject(method = "setViewDistance", at = @At("RETURN"))
	private void dividedchunks$window(int renderDistance, CallbackInfo ci) {
		int min = CubeRenderWindow.min();
		if (min == Integer.MIN_VALUE) {
			return;
		}
		this.dividedchunks$windowMin = min;
		this.sectionGridSizeY = CubeRenderWindow.height();
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
