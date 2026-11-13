package net.iskaa303.dividedchunks.mixin;

import dev.kikugie.fletching_table.annotation.MixinIgnore;
import net.iskaa303.dividedchunks.DividedChunks;
import net.iskaa303.dividedchunks.core.world.WorldConverter;
import net.iskaa303.dividedchunks.core.world.WorldKind;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;

/** Labels each world with the store it uses, and converts a vanilla world before it opens. */
@MixinIgnore
@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.WorldSelectionList$WorldListEntry")
public abstract class WorldListEntryMixin {

	@Shadow
	@Final
	private WorldSelectionList list;

	@Shadow
	@Final
	private LevelSummary summary;

	@Unique
	private WorldKind dividedchunks$kind;

	@Unique
	private Path dividedchunks$directory;

	@Unique
	private WorldKind dividedchunks$kind() {
		if (this.dividedchunks$kind == null) {
			this.dividedchunks$directory = Minecraft.getInstance().getLevelSource().getBaseDir()
					.resolve(this.summary.getLevelId());
			this.dividedchunks$kind = WorldKind.of(this.dividedchunks$directory);
		}
		return this.dividedchunks$kind;
	}

	@Inject(method = "extractContent", at = @At("TAIL"))
	private void dividedchunks$drawKind(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a,
			CallbackInfo ci) {
		WorldKind kind = this.dividedchunks$kind();
		if (kind == WorldKind.EMPTY) {
			return;
		}
		// no enum switch: its synthetic map class would sit in the mixin package, which Mixin refuses to load
		boolean cubic = kind == WorldKind.CUBIC;
		Component label = Component.translatable("dividedchunks.world.kind." + (cubic ? "cubic" : "vanilla"))
				.withStyle(cubic ? ChatFormatting.AQUA : ChatFormatting.GOLD);
		WorldSelectionList.Entry self = (WorldSelectionList.Entry) (Object) this;
		var font = Minecraft.getInstance().font;
		graphics.text(font, label, self.getContentRight() - font.width(label) - 2, self.getContentY() + 1, -1);
	}

	/** A vanilla world cannot open as it is, so offer conversion instead. */
	@Inject(method = "joinWorld", at = @At("HEAD"), cancellable = true)
	private void dividedchunks$convertBeforeJoin(CallbackInfo ci) {
		if (this.dividedchunks$kind() != WorldKind.VANILLA) {
			return;
		}
		ci.cancel();
		Minecraft minecraft = Minecraft.getInstance();
		Screen parent = this.list.getScreen();
		dividedchunks$show(minecraft, new ConfirmScreen(
				convert -> {
					if (!convert) {
						dividedchunks$show(minecraft, parent);
						return;
					}
					dividedchunks$show(minecraft, new ProgressScreen(true));
					Util.backgroundExecutor().execute(() -> this.dividedchunks$convert(minecraft, parent));
				},
				Component.translatable("dividedchunks.convert.title"),
				Component.translatable("dividedchunks.convert.message"),
				Component.translatable("dividedchunks.convert.confirm"),
				Component.translatable("dividedchunks.convert.cancel")));
	}

	@Unique
	private void dividedchunks$convert(Minecraft minecraft, Screen parent) {
		WorldConverter.Report report;
		try {
			report = WorldConverter.convert(this.dividedchunks$directory, WorldConverter.Progress.NONE);
		} catch (RuntimeException e) {
			DividedChunks.LOGGER.error("Failed to convert {}", this.dividedchunks$directory, e);
			minecraft.execute(() -> dividedchunks$show(minecraft, new AlertScreen(
					() -> dividedchunks$show(minecraft, parent),
					Component.translatable("dividedchunks.convert.failed"),
					Component.literal(String.valueOf(e.getMessage())))));
			return;
		}
		DividedChunks.LOGGER.info("Converted {}: {} chunks, {} sections, {} entity columns, {} point-of-interest columns",
				this.dividedchunks$directory.getFileName(), report.chunks(), report.sections(), report.entities(),
				report.pointsOfInterest());
		minecraft.execute(() -> {
			this.dividedchunks$kind = WorldKind.CUBIC;
			dividedchunks$show(minecraft, parent);
			((WorldSelectionList.WorldListEntry) (Object) this).joinWorld();
		});
	}

	@Unique
	private static void dividedchunks$show(Minecraft minecraft, Screen screen) {
		//? if >=26.2 {
		minecraft.gui.setScreen(screen);
		//?} else {
		/*minecraft.setScreen(screen);
		*///?}
	}
}
