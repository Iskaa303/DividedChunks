package net.iskaa303.dividedchunks.mixin;

import net.iskaa303.dividedchunks.DividedChunks;
import net.iskaa303.dividedchunks.core.config.DividedChunksConfig;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Widens {@code isInsideBuildHeight} to the dimension's configured range, so the level's block
 * accessors (guarded by {@code isInValidBounds}) accept positions outside the vanilla band.
 * {@code isOutsideBuildHeight} stays on the vanilla band, because vanilla uses it as the "outside
 * the rendered band" proxy (e.g. {@code LevelLoadTracker}); {@code getMinY}/{@code getMaxY} stay
 * vanilla too.
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
}
