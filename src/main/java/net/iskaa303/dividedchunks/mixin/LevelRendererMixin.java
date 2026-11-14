package net.iskaa303.dividedchunks.mixin;

import dev.kikugie.fletching_table.annotation.MixinIgnore;
import net.iskaa303.dividedchunks.core.client.CubeRenderWindow;
import net.iskaa303.dividedchunks.network.ClientCubeReceiver;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Builds the render band from the cube window instead of the vanilla band. */
@MixinIgnore
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

	//? if >=26.2 {
	@ModifyArg(
			method = "invalidateCompiledGeometry",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/ViewArea;<init>(Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;IIIIILnet/minecraft/client/renderer/SectionOcclusionGraph;)V"),
			index = 1)
	private int dividedchunks$windowMinBlockY(int minBlockY) {
		return CubeRenderWindow.minBlockY(minBlockY);
	}

	@ModifyArg(
			method = "invalidateCompiledGeometry",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/ViewArea;<init>(Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;IIIIILnet/minecraft/client/renderer/SectionOcclusionGraph;)V"),
			index = 2)
	private int dividedchunks$windowMaxBlockY(int maxBlockY) {
		return CubeRenderWindow.maxBlockY(maxBlockY);
	}

	@ModifyArg(
			method = "invalidateCompiledGeometry",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/ViewArea;<init>(Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;IIIIILnet/minecraft/client/renderer/SectionOcclusionGraph;)V"),
			index = 3)
	private int dividedchunks$windowMinSectionY(int minSectionY) {
		return CubeRenderWindow.minSectionY(minSectionY);
	}

	@ModifyArg(
			method = "invalidateCompiledGeometry",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/ViewArea;<init>(Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;IIIIILnet/minecraft/client/renderer/SectionOcclusionGraph;)V"),
			index = 4)
	private int dividedchunks$windowMaxSectionY(int maxSectionY) {
		return CubeRenderWindow.maxSectionY(maxSectionY);
	}
	//?} else {
	/*@Inject(method = "cullTerrain", at = @At("HEAD"))
	private void dividedchunks$applyHeldCubes(CallbackInfo ci) {
		ClientCubeReceiver.applyHeld(net.minecraft.client.Minecraft.getInstance().level);
	}
	*///?}
}
