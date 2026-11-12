package net.iskaa303.dividedchunks.mixin;

import dev.kikugie.fletching_table.annotation.MixinIgnore;
import net.iskaa303.dividedchunks.core.client.CubeBorderLines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla's line shader fogs lines out; the overlay needs them visible at the far render distance. */
@MixinIgnore
@Mixin(RenderTypes.class)
public abstract class RenderTypesMixin {

	@Inject(method = "lines", at = @At("RETURN"), cancellable = true)
	private static void dividedchunks$noFogLines(CallbackInfoReturnable<RenderType> cir) {
		cir.setReturnValue(CubeBorderLines.OPAQUE);
	}
}
