package net.iskaa303.dividedchunks.core.world;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorldKindTest {

	@TempDir
	Path world;

	@Test
	void cubeRegionsMakeItCubic() throws IOException {
		Files.createDirectories(this.world.resolve("dimensions/minecraft/overworld/dividedchunks"));
		Files.writeString(this.world.resolve("dimensions/minecraft/overworld/dividedchunks/r.0.0.dcr"), "x");
		assertEquals(WorldKind.CUBIC, WorldKind.of(this.world));
	}

	@Test
	void overworldRegionFilesMakeItVanilla() throws IOException {
		Files.createDirectories(this.world.resolve("region"));
		Files.writeString(this.world.resolve("region/r.0.0.mca"), "x");
		assertEquals(WorldKind.VANILLA, WorldKind.of(this.world));
	}

	@Test
	void dimensionRegionFilesMakeItVanilla() throws IOException {
		Files.createDirectories(this.world.resolve("dimensions/minecraft/the_nether/region"));
		Files.writeString(this.world.resolve("dimensions/minecraft/the_nether/region/r.0.0.mca"), "x");
		assertEquals(WorldKind.VANILLA, WorldKind.of(this.world));
	}

	@Test
	void cubesWinWhenBothKindsArePresent() throws IOException {
		Files.createDirectories(this.world.resolve("dimensions/minecraft/overworld/region"));
		Files.writeString(this.world.resolve("dimensions/minecraft/overworld/region/r.0.0.mca"), "x");
		Files.createDirectories(this.world.resolve("dimensions/minecraft/overworld/dividedchunks"));
		Files.writeString(this.world.resolve("dimensions/minecraft/overworld/dividedchunks/r.0.0.dcr"), "x");
		assertEquals(WorldKind.CUBIC, WorldKind.of(this.world));
	}

	@Test
	void anEmptyOrMissingDirectoryIsNeither() throws IOException {
		assertEquals(WorldKind.EMPTY, WorldKind.of(this.world));
		String absent = this.world.resolve("nope").toString();
		assertEquals(WorldKind.EMPTY, WorldKind.of(Path.of(absent)));
	}
}
