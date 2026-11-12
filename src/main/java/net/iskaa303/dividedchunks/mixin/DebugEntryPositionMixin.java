package net.iskaa303.dividedchunks.mixin;

import dev.kikugie.fletching_table.annotation.MixinIgnore;
import net.iskaa303.dividedchunks.core.world.CubeStores;
import net.minecraft.client.gui.components.debug.DebugEntryPosition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Locale;

/** The F3 position line names the file a column is stored in, which for this mod is a cube region. */
@MixinIgnore
@Mixin(DebugEntryPosition.class)
public abstract class DebugEntryPositionMixin {

	@Redirect(
			method = "display",
			at = @At(
					value = "INVOKE",
					target = "Ljava/lang/String;format(Ljava/util/Locale;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"))
	private static String dividedchunks$regionFileName(Locale locale, String format, Object[] args) {
		String text = String.format(locale, format, args);
		if (!text.contains(".mca")) {
			return text;
		}
		// Vanilla names the region the chunk sits in inside a .mca. Here a column is stored in a cube region,
		// and the cube (chunk x, section y, chunk z) is what indexes its records, so the line labels it as such.
		return String.format(Locale.ROOT, "Cube: %d %d %d [%s/r.%d.%d.dcr]",
				args[0], args[1], args[2], CubeStores.STORE_FOLDER, args[5], args[6]);
	}
}
