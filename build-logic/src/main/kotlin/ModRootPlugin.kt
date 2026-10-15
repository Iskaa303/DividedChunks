import dev.kikugie.stonecutter.controller.StonecutterControllerExtension
import me.modmuss50.mpp.ModPublishExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

class ModRootPlugin : Plugin<Project> {
	override fun apply(project: Project) {
		with(project) {
			val stonecutter = extensions.getByType<StonecutterControllerExtension>()
			val properties = stonecutter.properties
			val modVersion = properties.get<String>("mod.version")
			val channelTag = properties.get<String>("mod.channel_tag")
			val changelogText = file("CHANGELOG.md").takeIf { it.exists() }?.readText() ?: ""
			val githubToken = providers.environmentVariable("GITHUB_TOKEN")

			extensions.configure<ModPublishExtension>("publishMods") {
				dryRun = envTrue("PUB_DRY_RUN") || !envTrue("PUB_GITHUB_ENABLE")
				version = modVersion
				changelog.set(changelogText)
				type = releaseTypeFromChannelTag(channelTag)
				if (envTrue("PUB_GITHUB_ENABLE")) {
					github {
						accessToken = githubToken
						repository = providers.environmentVariable("GITHUB_REPOSITORY")
						commitish = providers.environmentVariable("GITHUB_SHA").orElse("main")
						tagName = providers.environmentVariable("GITHUB_REF_NAME")
						allowEmptyFiles = true
					}
				}
			}

			stonecutter.tasks {
				order("publishModrinth")
				order("publishCurseforge")
			}

			tasks.register("runActiveClient") {
				group = "stonecutter"
				description = "Run client of the active Stonecutter version"
				dependsOn(stonecutter.current!!.project + ":runClient")
			}
			tasks.register("runActiveServer") {
				group = "stonecutter"
				description = "Run server of the active Stonecutter version"
				dependsOn(stonecutter.current!!.project + ":runServer")
			}

			// In-game GameTests run one server at a time (each binds the default port).
			tasks.register("runGameTests") {
				group = "verification"
				description = "Runs the in-game GameTests for every Minecraft version and loader"
				dependsOn(stonecutter.versions.map { "${it.project}:runGameTestServer" })
			}

			for (version in stonecutter.versions.map { it.version }.distinct()) tasks.register("publish$version") {
				group = "publishing"
				dependsOn(stonecutter.tasks.named("publishMods") { metadata.version == version })
			}

			// One universal jar per Minecraft version, merging the Fabric and NeoForge jars for it.
			val baseVersion = "$modVersion$channelTag"
			val snapshotSuffix = if (envTrue("MOD_IS_RELEASE")) "" else "-SNAPSHOT"
			val mergeTasks = stonecutter.versions.map { it.version }.distinct().map { version ->
				val fabricProject = project(":$version-fabric")
				val neoforgeProject = project(":$version-neoforge")
				tasks.register<MergeLoadersTask>("mergeJars${version.replace(".", "")}") {
					group = "forgix"
					description = "Merges the Fabric and NeoForge jars for Minecraft $version"
					dependsOn(":$version-fabric:shadowJar", ":$version-neoforge:jar")
					fabricJar.set(fabricProject.provider { fabricProject.tasks.named("shadowJar", Jar::class.java).get().archiveFile.get() })
					neoforgeJar.set(neoforgeProject.provider { neoforgeProject.tasks.named("jar", Jar::class.java).get().archiveFile.get() })
					outputJar.set(layout.buildDirectory.file("forgix/dividedchunks-$baseVersion-universal+$version$snapshotSuffix.jar"))
				}
			}
			tasks.register("mergeJars") {
				group = "forgix"
				description = "Merges every Minecraft version's Fabric and NeoForge jars"
				dependsOn(mergeTasks)
			}

			// The root project owns build/forgix and build/libs but has no built-in clean task.
			tasks.register<Delete>("clean") {
				group = "build"
				description = "Deletes the root build directory (merged jars, collected jars, reports)"
				delete(layout.buildDirectory)
			}
		}
	}
}
