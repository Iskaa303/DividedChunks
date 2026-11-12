package net.iskaa303.dividedchunks.mixin;

import dev.kikugie.fletching_table.annotation.MixinIgnore;
import net.iskaa303.dividedchunks.core.client.CubeRenderWindow;
import net.iskaa303.dividedchunks.network.ClientCubeReceiver;
import net.minecraft.client.renderer.Octree;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drops the sections the occlusion Octree offers outside the vertical view distance: its box is a
 * power-of-two cube, taller than the view distance for radii that are not powers of two.
 */
@MixinIgnore
@Mixin(targets = "net.minecraft.client.renderer.SectionOcclusionGraph")
public abstract class SectionOcclusionGraphMixin {

	/**
	 * A cube that fills a section outside the vanilla band is invisible to vanilla, whose empty-section
	 * set only changes when a chunk packet arrives, so the graph is told before it walks the octree.
	 */
	@Inject(method = "update", at = @At("HEAD"))
	private void dividedchunks$cubeContentChanged(CallbackInfo ci) {
		ClientCubeReceiver.applyContentChanges((SectionOcclusionGraph) (Object) this);
	}

	@Redirect(
			method = "addSectionsInFrustum",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/Octree;visitNodes(Lnet/minecraft/client/renderer/Octree$OctreeVisitor;Lnet/minecraft/client/renderer/culling/Frustum;I)V"))
	private void dividedchunks$visitWithinCube(Octree tree, Octree.OctreeVisitor visitor, Frustum frustum,
			int closeDistance) {
		int radius = CubeRenderWindow.radius();
		int cameraSectionY = CubeRenderWindow.cameraSectionY();
		int[] culled = new int[1];
		tree.visitNodes((node, fullyVisible, depth, isClose) -> {
			SectionRenderDispatcher.RenderSection section = node.getSection();
			if (section != null
					&& Math.abs(SectionPos.y(section.getSectionNode()) - cameraSectionY) <= radius) {
				visitor.visit(node, fullyVisible, depth, isClose);
			} else if (section != null) {
				culled[0]++;
			}
		}, frustum, closeDistance);
		CubeRenderWindow.countCulled(culled[0]);
	}
}
