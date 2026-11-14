package net.iskaa303.dividedchunks.mixin;

import dev.kikugie.fletching_table.annotation.MixinIgnore;
//? if <26.2 {
/*import net.iskaa303.dividedchunks.core.client.CubeRenderWindow;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Anchors the occlusion Octree's vertical box at the render window instead of the world bottom, which
 * vanilla only does while its box spans every section. From 26.2 the render band rotates instead of
 * being fixed, so the box already starts at the live band and vanilla's own bound is used.
 */
//? if <26.2 {
/*@MixinIgnore
@Mixin(targets = "net.minecraft.client.renderer.SectionOcclusionGraph$GraphStorage")
public abstract class GraphStorageMixin {

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
*///?} else {
@MixinIgnore
@Mixin(net.minecraft.client.renderer.LevelRenderer.class)
public abstract class GraphStorageMixin {
}
//?}
