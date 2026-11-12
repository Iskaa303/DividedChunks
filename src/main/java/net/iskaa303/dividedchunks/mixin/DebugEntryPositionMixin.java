package net.iskaa303.dividedchunks.mixin;

import dev.kikugie.fletching_table.annotation.MixinIgnore;
import net.minecraft.client.gui.components.debug.DebugEntryPosition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Locale;

/** The F3 position line names the chunk's file, and this mod's chunks live in {@code .dcr} regions. */
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
		return text.contains(".mca") ? text.replace(".mca", ".dcr") : text;
	}
}
