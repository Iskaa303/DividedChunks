package net.iskaa303.dividedchunks.mixin;

import dev.kikugie.fletching_table.annotation.MixinIgnore;
import net.iskaa303.dividedchunks.core.client.CubeRenderWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Anchors the occlusion Octree's vertical box at the render window instead of the world bottom.
 * The Octree only starts at the world bottom while its box spans every section, which stops being
 * true once the window has moved off the vanilla band; without this the camera sits outside the box
 * and no out-of-band section is ever visited, so none gets compiled or drawn.
 */
@MixinIgnore
@Mixin(targets = "net.minecraft.client.renderer.SectionOcclusionGraph$GraphStorage")
public abstract class SectionOcclusionGraphMixin {

	@ModifyArg(
			method = "<init>",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/Octree;<init>(Lnet/minecraft/core/SectionPos;III)V"),
			index = 3)
	private int dividedchunks$octreeMinBlockY(int minBlockY) {
		return CubeRenderWindow.octreeMinBlockY(minBlockY);
	}
}
