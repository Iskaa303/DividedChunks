package net.iskaa303.dividedchunks.mixin;

import dev.kikugie.fletching_table.annotation.MixinIgnore;
import net.iskaa303.dividedchunks.core.client.CubeRenderWindow;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Rebuilds the render band once the player has flown far enough vertically. */
//? if >=26.2 {
@MixinIgnore
@Mixin(net.minecraft.client.renderer.extract.LevelExtractor.class)
public abstract class LevelExtractorMixin {

	@Shadow
	private boolean shouldInvalidateCompiledGeometry;

	@Inject(method = "extract", at = @At("HEAD"))
	private void dividedchunks$rebuildOnVerticalDrift(CallbackInfo ci) {
		if (CubeRenderWindow.needsRebuild()) {
			this.shouldInvalidateCompiledGeometry = true;
		}
	}
}
//?} else {
/*@MixinIgnore
@Mixin(LevelRenderer.class)
public abstract class LevelExtractorMixin {
}
*///?}
