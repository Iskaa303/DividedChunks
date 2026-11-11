package net.iskaa303.dividedchunks.mixin;

import dev.kikugie.fletching_table.annotation.MixinIgnore;
import net.iskaa303.dividedchunks.core.client.CubeRenderWindow;
import net.minecraft.client.renderer.Octree;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Drops the sections the occlusion Octree offers outside the vertical view distance, so what is drawn
 * is a cube around the player. The Octree's own box is a cube the size of the render distance in
 * sections, which is taller than the view distance for every radius that is not a power of two.
 */
@MixinIgnore
@Mixin(targets = "net.minecraft.client.renderer.SectionOcclusionGraph")
public abstract class SectionOcclusionGraphMixin {

	@Redirect(
			method = "addSectionsInFrustum",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/Octree;visitNodes(Lnet/minecraft/client/renderer/Octree$OctreeVisitor;Lnet/minecraft/client/renderer/culling/Frustum;I)V"))
	private void dividedchunks$visitWithinCube(Octree tree, Octree.OctreeVisitor visitor, Frustum frustum,
			int closeDistance) {
		int radius = CubeRenderWindow.radius();
		int cameraSectionY = CubeRenderWindow.cameraSectionY();
		tree.visitNodes((node, fullyVisible, depth, isClose) -> {
			SectionRenderDispatcher.RenderSection section = node.getSection();
			if (section != null
					&& Math.abs(SectionPos.y(section.getSectionNode()) - cameraSectionY) <= radius) {
				visitor.visit(node, fullyVisible, depth, isClose);
			}
		}, frustum, closeDistance);
	}
}
