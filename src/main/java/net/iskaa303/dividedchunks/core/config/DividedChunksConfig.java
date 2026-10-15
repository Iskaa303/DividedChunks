package net.iskaa303.dividedchunks.core.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import net.iskaa303.dividedchunks.DividedChunks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mod configuration, stored as {@code config/dividedchunks.toml}.
 *
 * <p>One global section plus per-dimension overrides; a dimension without an entry falls
 * back to {@link #defaults}. The file is generated with comments and re-normalised on
 * every load, so a hand-edited value is kept but the shape always stays valid. Loading a
 * malformed file logs a warning and falls back to defaults rather than failing startup.</p>
 */
public final class DividedChunksConfig {

	/** Lowest block Y the mod accepts; lower values are clamped up to this. */
	public static final int MIN_ALLOWED_Y = Integer.MIN_VALUE;
	/** Highest block Y the mod accepts; higher values are clamped down to this. */
	public static final int MAX_ALLOWED_Y = Integer.MAX_VALUE;
	/** Defaults to the full accepted range, i.e. effectively unbounded (the whole 32-bit int range). */
	public static final int DEFAULT_MIN_Y = MIN_ALLOWED_Y;
	public static final int DEFAULT_MAX_Y = MAX_ALLOWED_Y;
	public static final int MAX_SKY_CONE_ANGLE_DEGREES = 80;

	private static final String FILE_NAME = "dividedchunks.toml";

	private static final String KEY_VERTICAL_VIEW_DISTANCE = "verticalViewDistanceCubes";
	private static final String KEY_DEFAULTS = "defaults";
	private static final String KEY_DIMENSIONS = "dimensions";
	private static final String KEY_MIN_Y = "minY";
	private static final String KEY_MAX_Y = "maxY";
	private static final String KEY_SKY_CONE = "skyConeAngleDegrees";

	private static final String COMMENT_HEADER =
			"DividedChunks configuration.\n"
					+ "Values under [defaults] apply to every dimension unless that dimension has its own entry.";
	private static final String COMMENT_VERTICAL_VIEW_DISTANCE =
			"How many cubes above and below the player's cube stay loaded.";
	private static final String COMMENT_MIN_Y =
			"Lowest block Y that exists. The accepted range is the full signed 32-bit int range\n"
					+ "(-2147483648 to 2147483647); values outside it are clamped to the closest accepted value, and\n"
					+ "every value is snapped to a cube boundary. Defaults to the full range, i.e. effectively\n"
					+ "unbounded.";
	private static final String COMMENT_MAX_Y =
			"Highest block Y that exists. Same accepted range and clamping as minY (see above).";
	private static final String COMMENT_SKY_CONE =
			"Half-angle of the sky-light cone in degrees. 0 = straight down (vanilla shadow); larger values\n"
					+ "let sky light arrive from the sides, so a distant structure only partially shadows the ground.";

	private static final TomlWriter WRITER = new TomlWriter();

	/** NightConfig writes comments as {@code #text}; prefix each line with a space for readability. */
	private static String commentText(String text) {
		return " " + text.replace("\n", "\n ");
	}

	/** How many cubes above and below the player's cube stay loaded, per dimension. */
	public int verticalViewDistanceCubes = 8;

	/** Settings used by any dimension without its own entry. */
	public DimensionConfig defaults = new DimensionConfig();

	/** Per-dimension overrides, keyed by dimension identifier, e.g. {@code minecraft:overworld}. */
	public Map<String, DimensionConfig> dimensions = new LinkedHashMap<>();

	/** Effective settings for the given dimension identifier. Never {@code null}. */
	public DimensionConfig forDimension(String dimensionId) {
		DimensionConfig override = this.dimensions.get(dimensionId);
		return override != null ? override : this.defaults;
	}

	/** Clamps every value into a range the rest of the mod can rely on. */
	public void sanitize() {
		if (this.defaults == null) {
			this.defaults = new DimensionConfig();
		}
		if (this.dimensions == null) {
			this.dimensions = new LinkedHashMap<>();
		}
		this.verticalViewDistanceCubes = Math.max(0, this.verticalViewDistanceCubes);
		this.defaults.sanitize();
		for (DimensionConfig dimension : this.dimensions.values()) {
			if (dimension != null) {
				dimension.sanitize();
			}
		}
	}

	/** Loads {@code config/dividedchunks.toml}, writing defaults (with comments) if needed. */
	public static DividedChunksConfig load(Path configDir) {
		Path file = configDir.resolve(FILE_NAME);
		DividedChunksConfig config = Files.isRegularFile(file) ? read(file) : new DividedChunksConfig();
		config.sanitize();
		config.save(configDir);
		return config;
	}

	private static DividedChunksConfig read(Path file) {
		DividedChunksConfig config = new DividedChunksConfig();
		try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			config.readFrom(new TomlParser().parse(reader));
		} catch (IOException | RuntimeException e) {
			DividedChunks.LOGGER.warn("Could not read {}, using defaults: {}", file, e.toString());
			return new DividedChunksConfig();
		}
		return config;
	}

	private void readFrom(UnmodifiableConfig toml) {
		this.verticalViewDistanceCubes = toml.getIntOrElse(KEY_VERTICAL_VIEW_DISTANCE, this.verticalViewDistanceCubes);

		UnmodifiableConfig def = toml.get(KEY_DEFAULTS);
		if (def != null) {
			this.defaults.readFrom(def);
		}

		UnmodifiableConfig dims = toml.get(KEY_DIMENSIONS);
		if (dims != null) {
			for (UnmodifiableConfig.Entry entry : dims.entrySet()) {
				if (entry.getValue() instanceof UnmodifiableConfig dimension) {
					DimensionConfig config = new DimensionConfig();
					config.readFrom(dimension);
					this.dimensions.put(entry.getKey(), config);
				}
			}
		}
	}

	/** Writes the config, creating {@code configDir} if needed. Failures are logged, not thrown. */
	public void save(Path configDir) {
		Path file = configDir.resolve(FILE_NAME);
		try {
			Files.createDirectories(configDir);
			try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
				for (String line : COMMENT_HEADER.split("\n")) {
					writer.write("# ");
					writer.write(line);
					writer.write(System.lineSeparator());
				}
				WRITER.write(toToml(), writer);
			}
		} catch (IOException e) {
			DividedChunks.LOGGER.error("Could not write {}", file, e);
		}
	}

	private CommentedConfig toToml() {
		CommentedConfig toml = CommentedConfig.inMemory();
		toml.set(KEY_VERTICAL_VIEW_DISTANCE, this.verticalViewDistanceCubes);
		toml.setComment(KEY_VERTICAL_VIEW_DISTANCE, commentText(COMMENT_VERTICAL_VIEW_DISTANCE));
		toml.set(KEY_DEFAULTS, this.defaults.toToml());
		CommentedConfig dims = toml.createSubConfig();
		for (Map.Entry<String, DimensionConfig> entry : this.dimensions.entrySet()) {
			dims.set(entry.getKey(), entry.getValue().toToml());
		}
		toml.set(KEY_DIMENSIONS, dims);
		return toml;
	}

	/** One-line summary for the startup log. */
	public String describe() {
		return "verticalViewDistanceCubes=" + this.verticalViewDistanceCubes
				+ ", dimensions=" + this.dimensions.size()
				+ ", defaultY=[" + this.defaults.minY + ", " + this.defaults.maxY + "]"
				+ ", skyConeAngleDegrees=" + this.defaults.skyConeAngleDegrees;
	}

	/** Per-dimension settings. {@code minY}/{@code maxY} are inclusive block coordinates. */
	public static final class DimensionConfig {

		public int minY = DEFAULT_MIN_Y;
		public int maxY = DEFAULT_MAX_Y;
		public int skyConeAngleDegrees = 0;

		private void readFrom(UnmodifiableConfig toml) {
			this.minY = toml.getIntOrElse(KEY_MIN_Y, this.minY);
			this.maxY = toml.getIntOrElse(KEY_MAX_Y, this.maxY);
			this.skyConeAngleDegrees = toml.getIntOrElse(KEY_SKY_CONE, this.skyConeAngleDegrees);
		}

		private CommentedConfig toToml() {
			CommentedConfig toml = CommentedConfig.inMemory();
			toml.set(KEY_MIN_Y, this.minY);
			toml.setComment(KEY_MIN_Y, commentText(COMMENT_MIN_Y));
			toml.set(KEY_MAX_Y, this.maxY);
			toml.setComment(KEY_MAX_Y, commentText(COMMENT_MAX_Y));
			toml.set(KEY_SKY_CONE, this.skyConeAngleDegrees);
			toml.setComment(KEY_SKY_CONE, commentText(COMMENT_SKY_CONE));
			return toml;
		}

		public void sanitize() {
			// Clamp into the accepted range, then snap to cube boundaries so every section is
			// either fully in or fully out.
			int alignedMin = clampToAcceptedRange((this.minY >> 4) << 4);
			int alignedMax = clampToAcceptedRange(((this.maxY >> 4) << 4) | 15);
			if (alignedMax <= alignedMin) {
				alignedMin = DEFAULT_MIN_Y;
				alignedMax = DEFAULT_MAX_Y;
			}
			this.minY = alignedMin;
			this.maxY = alignedMax;
			this.skyConeAngleDegrees = Math.max(0, Math.min(MAX_SKY_CONE_ANGLE_DEGREES, this.skyConeAngleDegrees));
		}

		private static int clampToAcceptedRange(int y) {
			return Math.max(MIN_ALLOWED_Y, Math.min(MAX_ALLOWED_Y, y));
		}
	}
}
