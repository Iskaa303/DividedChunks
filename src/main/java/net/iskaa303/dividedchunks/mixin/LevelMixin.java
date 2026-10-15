package net.iskaa303.dividedchunks.mixin;

import net.iskaa303.dividedchunks.DividedChunks;
import net.iskaa303.dividedchunks.core.config.DividedChunksConfig;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Widens {@code isInsideBuildHeight}/{@code isOutsideBuildHeight} to the dimension's configured
 * range, so a block can be placed outside the vanilla band.
 *
 * <p>{@code getMinY}/{@code getMaxY} and the whole section band stay on {@code dimensionType()}, so
 * heightmaps, light, spawning and every other loop bounded by the min/max Y keep vanilla
 * behaviour. Sections outside the band are reached by section Y instead.</p>
 */
@Mixin(Level.class)
public abstract class LevelMixin {

	@Unique
	private DividedChunksConfig.DimensionConfig dividedchunks$config() {
		return DividedChunks.config().forDimension(((Level) (Object) this).dimension().identifier().toString());
	}

	public boolean isInsideBuildHeight(int blockY) {
		DividedChunksConfig.DimensionConfig config = this.dividedchunks$config();
		return blockY >= config.minY && blockY <= config.maxY;
	}

	public boolean isOutsideBuildHeight(int blockY) {
		return !this.isInsideBuildHeight(blockY);
	}
}
