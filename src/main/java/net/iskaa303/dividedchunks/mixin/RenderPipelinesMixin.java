package net.iskaa303.dividedchunks.mixin;

//? if >=26.3 {
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.kikugie.fletching_table.annotation.MixinIgnore;
import net.iskaa303.dividedchunks.core.client.CubeBorderLines;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/** Vanilla compiles only the pipelines listed here, so the overlay's own pipeline has to be added. */
@MixinIgnore
@Mixin(RenderPipelines.class)
public abstract class RenderPipelinesMixin {

	@Inject(method = "requiredPipelines", at = @At("RETURN"), cancellable = true)
	private static void dividedchunks$addCubeBorderPipelines(CallbackInfoReturnable<List<RenderPipeline>> cir) {
		List<RenderPipeline> pipelines = new ArrayList<>(cir.getReturnValue());
		pipelines.add(CubeBorderLines.PIPELINE);
		cir.setReturnValue(List.copyOf(pipelines));
	}
}
// Before 26.3 line pipelines are compiled on first use, so there is nothing to register.
//?} else {
/*import dev.kikugie.fletching_table.annotation.MixinIgnore;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;

@MixinIgnore
@Mixin(RenderPipelines.class)
public abstract class RenderPipelinesMixin {
}
*///?}
