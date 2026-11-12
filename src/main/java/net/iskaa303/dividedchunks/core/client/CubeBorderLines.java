package net.iskaa303.dividedchunks.core.client;

//? if >=26.3 {
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
//?} else {
/*import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
*///?}
import net.iskaa303.dividedchunks.DividedChunks;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * Fog-free line type for the overlay. Vanilla's line shader blends into the fog colour, which hides
 * the far end of every column in the sky.
 */
public final class CubeBorderLines {

	/** 26.3 reads {@code #include}, 26.1.2 and 26.2 read {@code #moj_import}, so each dialect has its own file. */
	//? if >=26.3 {
	private static final String FRAGMENT_SHADER = "line_nofog_include";
	//?} else {
	/*private static final String FRAGMENT_SHADER = "line_nofog_moj_import";
	*///?}

	public static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath(DividedChunks.MOD_ID, "pipeline/cube_border_lines"))
			.withFragmentShader(Identifier.fromNamespaceAndPath(DividedChunks.MOD_ID, "core/" + FRAGMENT_SHADER))
			.withColorTargetState(ColorTargetState.DEFAULT)
			.withDepthStencilState(DepthStencilState.DEFAULT)
			.build();

	/** The overlay uses opaque colours only, so the translucent line type is left to vanilla. */
	public static final RenderType OPAQUE = RenderType.create("dividedchunks_cube_borders",
			RenderSetup.builder(PIPELINE).setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
					.createRenderSetup());

	private CubeBorderLines() {
	}
}
