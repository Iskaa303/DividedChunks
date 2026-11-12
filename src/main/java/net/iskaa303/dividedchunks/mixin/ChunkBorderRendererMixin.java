package net.iskaa303.dividedchunks.mixin;

import dev.kikugie.fletching_table.annotation.MixinIgnore;
import net.iskaa303.dividedchunks.core.client.CubeDebugBorders;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.ChunkBorderRenderer;
import net.minecraft.core.SectionPos;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the cube lattice to vanilla's chunk borders. That renderer only exists while the borders are
 * toggled on, so the grid appears and disappears with them.
 */
@MixinIgnore
@Mixin(ChunkBorderRenderer.class)
public abstract class ChunkBorderRendererMixin {

	@Inject(method = "emitGizmos", at = @At("TAIL"))
	private void dividedchunks$emitCubeBorders(double camX, double camY, double camZ, DebugValueAccess debugValues,
			Frustum frustum, float partialTicks, CallbackInfo ci) {
		Minecraft minecraft = Minecraft.getInstance();
		Entity camera = minecraft.getCameraEntity();
		if (camera != null) {
			CubeDebugBorders.emit(SectionPos.of(camera.blockPosition()),
					minecraft.options.getEffectiveRenderDistance());
		}
	}
}
