package net.iskaa303.dividedchunks.mixin;

import dev.kikugie.fletching_table.annotation.MixinIgnore;
import net.iskaa303.dividedchunks.network.ClientCubeReceiver;
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
	private net.minecraft.client.multiplayer.ClientLevel level;

	@Inject(method = "extract", at = @At("HEAD"))
	private void dividedchunks$applyHeldCubes(CallbackInfo ci) {
		ClientCubeReceiver.applyHeld(this.level);
	}
}
//?} else {
/*@MixinIgnore
@Mixin(LevelRenderer.class)
public abstract class LevelExtractorMixin {
}
*///?}
