package net.iskaa303.dividedchunks.mixin;

import dev.kikugie.fletching_table.annotation.MixinIgnore;
import net.iskaa303.dividedchunks.core.client.CubeRenderWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Tracks section dirtiness over the render window instead of the vanilla band: outside the band
 * nothing else ever marks a section dirty, so out-of-band sections would never be compiled.
 */
//? if >=26.2 {
@MixinIgnore
@Mixin(net.minecraft.client.SectionUpdateTracker.class)
public abstract class SectionUpdateTrackerMixin {

	@ModifyArg(
			method = "<init>",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/RotatingSectionStorage;<init>(IIILnet/minecraft/client/RotatingSectionStorage$ValueCreator;)V"),
			index = 1)
	private int dividedchunks$windowMinSectionY(int minSectionY) {
		return CubeRenderWindow.minSectionY(minSectionY);
	}

	@ModifyArg(
			method = "<init>",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/RotatingSectionStorage;<init>(IIILnet/minecraft/client/RotatingSectionStorage$ValueCreator;)V"),
			index = 2)
	private int dividedchunks$windowMaxSectionY(int maxSectionY) {
		return CubeRenderWindow.maxSectionY(maxSectionY);
	}
}
//?} else {
/*@MixinIgnore
@Mixin(net.minecraft.client.renderer.LevelRenderer.class)
public abstract class SectionUpdateTrackerMixin {
}
*///?}
