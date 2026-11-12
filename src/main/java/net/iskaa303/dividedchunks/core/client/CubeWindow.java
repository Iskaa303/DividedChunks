package net.iskaa303.dividedchunks.core.client;

/**
 * The vertical band of render sections: a bucket of {@code step} sections around the player plus one
 * cube of margin at each end, so the drawn cube always fits and the band only moves when the player
 * leaves the bucket. Moving it drops every compiled section and recompiles what is visible, which is
 * why the bucket is as large as the render grid allows.
 */
record CubeWindow(int min, int max, int radius) {

	/** Smallest bucket; a smaller one rebuilds too often to be worth the saved memory. */
	private static final int MIN_STEP = 16;
	/** Largest band; the dense render grid costs its height times {@code (2 * radius + 1)^2} sections. */
	private static final int MAX_HEIGHT = 64;

	/** The band holding {@code sectionY}, anchored to world Y so it moves only on a bucket boundary. */
	static CubeWindow around(int sectionY, int radius) {
		int base = Math.floorDiv(sectionY, step(radius)) * step(radius);
		return new CubeWindow(base - radius, base + step(radius) + radius - 1, radius);
	}

	/** Height of a band for a render distance, in sections. */
	static int height(int radius) {
		return step(radius) + 2 * radius;
	}

	/** Sections the player may drift before the band has to move. */
	private static int step(int radius) {
		int largestStep = Math.max(MIN_STEP, MAX_HEIGHT - 2 * radius);
		return Math.min(Math.max(4 * radius, MIN_STEP), largestStep);
	}

	/** True while the player is still inside the bucket, which is the only time the cube is covered. */
	boolean holds(int sectionY) {
		return sectionY >= this.min + this.radius && sectionY <= this.max - this.radius;
	}

	int height() {
		return this.max - this.min + 1;
	}
}
