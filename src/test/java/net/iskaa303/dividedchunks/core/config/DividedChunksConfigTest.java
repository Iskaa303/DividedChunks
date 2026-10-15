package net.iskaa303.dividedchunks.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DividedChunksConfigTest {

	@Test
	void writesDefaultsWithCommentsWhenFileIsMissing(@TempDir Path dir) throws IOException {
		DividedChunksConfig config = DividedChunksConfig.load(dir);
		Path file = dir.resolve("dividedchunks.toml");
		assertTrue(Files.isRegularFile(file), "default config file should be written");

		String text = Files.readString(file, StandardCharsets.UTF_8);
		assertTrue(text.contains("DividedChunks configuration"), "file should have a header comment");
		assertTrue(text.contains("#"), "file should contain comments");
		assertTrue(text.contains("minY = -2147483648"), "default minY should be written: \n" + text);
		assertTrue(text.contains("skyConeAngleDegrees = 0"), "default sky cone angle should be written");

		assertEquals(DividedChunksConfig.DEFAULT_MIN_Y, config.defaults.minY);
		assertEquals(DividedChunksConfig.DEFAULT_MAX_Y, config.defaults.maxY);
		assertEquals(8, config.verticalViewDistanceCubes);
	}

	@Test
	void roundTripsDimensionOverrides(@TempDir Path dir) {
		DividedChunksConfig config = DividedChunksConfig.load(dir);
		DividedChunksConfig.DimensionConfig end = new DividedChunksConfig.DimensionConfig();
		end.minY = -64;
		end.maxY = 63;
		end.skyConeAngleDegrees = 30;
		config.dimensions.put("minecraft:the_end", end);
		config.save(dir);

		DividedChunksConfig reloaded = DividedChunksConfig.load(dir);
		DividedChunksConfig.DimensionConfig loaded = reloaded.forDimension("minecraft:the_end");
		assertEquals(-64, loaded.minY);
		assertEquals(63, loaded.maxY);
		assertEquals(30, loaded.skyConeAngleDegrees);
		// A dimension with no entry still gets the defaults.
		assertEquals(DividedChunksConfig.DEFAULT_MIN_Y, reloaded.forDimension("minecraft:overworld").minY);
	}

	@Test
	void sanitizeClampsOutOfRangeValues() {
		DividedChunksConfig config = new DividedChunksConfig();
		config.defaults.minY = Integer.MIN_VALUE;
		config.defaults.maxY = Integer.MAX_VALUE;
		config.sanitize();

		assertEquals(DividedChunksConfig.MIN_ALLOWED_Y, config.defaults.minY);
		assertEquals(DividedChunksConfig.MAX_ALLOWED_Y, config.defaults.maxY);
	}

	@Test
	void sanitizeAlignsAndClamps() {
		DividedChunksConfig config = new DividedChunksConfig();
		config.verticalViewDistanceCubes = -5;
		config.defaults.minY = -1;
		config.defaults.maxY = 0;
		config.defaults.skyConeAngleDegrees = 999;
		config.sanitize();

		assertEquals(0, config.verticalViewDistanceCubes);
		assertEquals(-16, config.defaults.minY);
		assertEquals(15, config.defaults.maxY);
		assertEquals(DividedChunksConfig.MAX_SKY_CONE_ANGLE_DEGREES, config.defaults.skyConeAngleDegrees);
	}

	@Test
	void malformedFileFallsBackToDefaults(@TempDir Path dir) throws IOException {
		Files.writeString(dir.resolve("dividedchunks.toml"), "this is = not [ valid toml", StandardCharsets.UTF_8);
		DividedChunksConfig config = DividedChunksConfig.load(dir);
		assertEquals(DividedChunksConfig.DEFAULT_MIN_Y, config.defaults.minY);
	}
}
