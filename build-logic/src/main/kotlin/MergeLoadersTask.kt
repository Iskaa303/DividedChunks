import io.github.pacifistmc.forgix.Forgix
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Merges one Minecraft version's Fabric and NeoForge jars into a single universal jar. */
abstract class MergeLoadersTask : DefaultTask() {

	@get:InputFiles
	@get:PathSensitive(PathSensitivity.NONE)
	abstract val fabricJar: RegularFileProperty

	@get:InputFiles
	@get:PathSensitive(PathSensitivity.NONE)
	abstract val neoforgeJar: RegularFileProperty

	@get:OutputFile
	abstract val outputJar: RegularFileProperty

	@TaskAction
	fun merge() {
		Forgix.mergeLoaders(
			linkedMapOf(
				this.fabricJar.get().asFile to "fabric",
				this.neoforgeJar.get().asFile to "neoforge"
			),
			this.outputJar.get().asFile,
			true
		)
	}
}
