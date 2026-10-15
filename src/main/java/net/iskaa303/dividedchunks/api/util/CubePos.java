package net.iskaa303.dividedchunks.api.util;

/** One 16&#215;16&#215;16 cube in cube coordinates; {@code y} is a section Y like {@code x}/{@code z}. */
public record CubePos(int x, int y, int z) {

	public static CubePos fromBlock(int blockX, int blockY, int blockZ) {
		return new CubePos(
				Coords.blockToCube(blockX),
				Coords.blockToCube(blockY),
				Coords.blockToCube(blockZ));
	}

	public int minBlockX() {
		return Coords.cubeToMinBlock(this.x);
	}

	public int minBlockY() {
		return Coords.cubeToMinBlock(this.y);
	}

	public int minBlockZ() {
		return Coords.cubeToMinBlock(this.z);
	}

	public int maxBlockX() {
		return Coords.cubeToMaxBlock(this.x);
	}

	public int maxBlockY() {
		return Coords.cubeToMaxBlock(this.y);
	}

	public int maxBlockZ() {
		return Coords.cubeToMaxBlock(this.z);
	}

	public CubePos offset(int dx, int dy, int dz) {
		return new CubePos(this.x + dx, this.y + dy, this.z + dz);
	}

	public CubePos above() {
		return new CubePos(this.x, this.y + 1, this.z);
	}

	public CubePos below() {
		return new CubePos(this.x, this.y - 1, this.z);
	}
}
