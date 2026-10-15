plugins {
	id("mod-platform")
	id("dev.kikugie.loom-back-compat")
	id("com.gradleup.shadow")
}

// NightConfig (TOML) is shipped by NeoForge/FML but not by Fabric Loader, so on Fabric it is
// shaded into the mod jar under a private package. LGPL-3.0; see licenses/night-config.txt.
val nightConfig = "com.electronwill.night-config:toml:3.9.0"
val nightConfigShaded = configurations.create("nightConfigShaded")

platform {
	loader = "fabric"
	jarTask.set("shadowJar")
	dependencies {
		required("minecraft") {
			fabricLikeVersionRange = prop("deps.minecraft")
		}
		required("fabric-api") {
			slug("fabric-api")
			fabricLikeVersionRange = ">=${prop("deps.fabric-api")}"
		}
		required("fabricloader") {
			fabricLikeVersionRange = ">=${prop("deps.fabric-loader")}"
		}
	}
}

loom {
	accessWidenerPath = rootProject.file("src/main/resources/aw/${sc.current.version}.accesswidener")
	val devJvmArgs = propsList("mod", "dev_jvm_args")
	runs.named("client") {
		client()
		generateRunConfig.set(true)
		runDirectory.set(layout.projectDirectory.dir("run"))
		programArguments.add("--username=Dev")
		jvmArguments.addAll(devJvmArgs)
		displayName.set("Fabric Client (${sc.current.version})")
	}
	runs.named("server") {
		server()
		generateRunConfig.set(true)
		runDirectory.set(layout.projectDirectory.dir("run"))
		jvmArguments.addAll(devJvmArgs)
		displayName.set("Fabric Server (${sc.current.version})")
	}
}

fabricApi {
	configureDataGeneration {
		outputDirectory = file("${rootDir}/versions/datagen/${sc.current.version}/src/main/generated")
		client = true
	}
}

repositories {
	mavenCentral()
	strictMaven("https://maven.terraformersmc.com/", "com.terraformersmc") { name = "TerraformersMC" }
}

configurations.all {
	resolutionStrategy {
		force("net.fabricmc:fabric-loader:${prop("deps.fabric-loader")}")
	}
}

dependencies {
	minecraft("com.mojang:minecraft:${prop("deps.minecraft")}")
	modImplementation("net.fabricmc:fabric-loader:${prop("deps.fabric-loader")}")
	modImplementation("net.fabricmc.fabric-api:fabric-api:${prop("deps.fabric-api")}")
	modLocalRuntime("com.terraformersmc:modmenu:${prop("deps.modmenu")}")
	implementation(nightConfig)
	add(nightConfigShaded.name, nightConfig)
}

// Relocate the TOML library into the mod's own namespace so it cannot clash with another Fabric
// mod shipping a different NightConfig, and so nothing is nested in META-INF/jars.
tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
	configurations = listOf(nightConfigShaded)
	relocate("com.electronwill.nightconfig", "DividedChunks.libraries.electronwill.nightconfig")
	mergeServiceFiles()
	archiveClassifier.set("")
	// NightConfig core carries Java 17 overrides of two serde classes; keep only the base versions
	// and drop the multi-release marker so the jar is a plain one.
	exclude("META-INF/versions/**")
	addMultiReleaseAttribute.set(false)
}

// The shaded jar replaces the plain one as the mod's artifact.
tasks.named("jar") { enabled = false }
tasks.named("assemble") { dependsOn(tasks.named("shadowJar")) }

// The Kotlin compiler lags the Java toolchain; pin its target so it does not warn about the fallback.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
	compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_24)
}
