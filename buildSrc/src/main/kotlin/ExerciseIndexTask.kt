import java.io.File
import java.security.MessageDigest
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = false
}

open class PrepareExerciseIndexTask : DefaultTask() {
    @get:InputDirectory
    val sourceDir: DirectoryProperty = project.objects.directoryProperty()

    @get:OutputDirectory
    val outputDir: DirectoryProperty = project.objects.directoryProperty()

    @get:Input
    val minimumInstructionCount: Property<Int> = project.objects.property(Int::class.java)

    init {
        minimumInstructionCount.convention(1)
    }

    @TaskAction
    fun generateIndex() {
        val sourceRoot = sourceDir.get().asFile
        val exercisesFile = File(sourceRoot, "src/data/exercises.json")
        require(exercisesFile.exists()) { "Unable to locate exercises.json in $sourceRoot" }

        val rawExercises = exercisesFile.reader().use {
            json.decodeFromString<List<RawExercise>>(it.readText())
        }

        val entries = rawExercises.mapNotNull { raw ->
            val id = raw.exerciseId?.takeIf { it.isNotBlank() } ?: raw.id ?: return@mapNotNull null
            val heroAsset = findHeroAsset(id)
            val mediaAssets = buildList {
                heroAsset?.let { add(it) }
                raw.imageUrls.orEmpty()
                    .mapNotNull { toRelativeMediaPath(it, id) }
                    .filter { it != heroAsset }
                    .forEach { add(it) }
            }

            val instructions = raw.instructions.ifEmpty { raw.secondaryInstructions.orEmpty() }
            if (instructions.size < minimumInstructionCount.get()) return@mapNotNull null

            val normalized = normalize(raw.name)
            val searchTerms = buildSet {
                addAll(normalized.split(" "))
                addAll(raw.alias.orEmpty().flatMap { normalize(it).split(" ") })
                addAll(raw.bodyParts.flatMap { normalize(it).split(" ") })
                addAll(raw.targetMuscles.flatMap { normalize(it).split(" ") })
                addAll(raw.secondaryMuscles.flatMap { normalize(it).split(" ") })
                addAll(raw.equipments.flatMap { normalize(it).split(" ") })
                raw.force?.let { addAll(normalize(it).split(" ")) }
                raw.mechanic?.let { addAll(normalize(it).split(" ")) }
                add(normalized.replace(" ", ""))
            }.toList()

            ExerciseIndexEntry(
                id = id,
                name = raw.name.trim(),
                heroAsset = heroAsset,
                mediaAssets = mediaAssets,
                instructions = instructions.map(String::trim),
                bodyParts = raw.bodyParts,
                targetMuscles = raw.targetMuscles,
                secondaryMuscles = raw.secondaryMuscles,
                equipments = raw.equipments,
                force = raw.force,
                mechanic = raw.mechanic,
                difficulty = raw.difficulty,
                alias = raw.alias.orEmpty(),
                category = raw.category,
                tips = raw.tips.orEmpty(),
                normalizedName = normalized,
                searchTerms = searchTerms,
                checksum = checksum(instructions.joinToString("|"))
            )
        }

        val outputDirectory = outputDir.get().asFile
        if (outputDirectory.exists()) {
            outputDirectory.deleteRecursively()
        }

        val containerDir = File(outputDirectory, "exercise_index")
        containerDir.mkdirs()

        val indexFile = File(containerDir, "exercise_library.json")
        indexFile.writeText(json.encodeToString(entries))

        val metadataFile = File(containerDir, "metadata.json")
        val metadata = ExerciseMetadata(
            count = entries.size,
            bodyParts = entries.flatMap { it.bodyParts }.toSortedSet().toList(),
            equipments = entries.flatMap { it.equipments }.toSortedSet().toList(),
            targetMuscles = entries.flatMap { it.targetMuscles }.toSortedSet().toList(),
            secondaryMuscles = entries.flatMap { it.secondaryMuscles }.toSortedSet().toList(),
        )
        metadataFile.writeText(json.encodeToString(metadata))
    }

    private fun findHeroAsset(id: String): String? {
        val sourceRoot = sourceDir.get().asFile
        val mediaDir = File(sourceRoot, "media")
        val gif = File(mediaDir, "$id.gif")
        if (gif.exists()) {
            return "media/${gif.name}"
        }
        val png = File(mediaDir, "$id.png")
        if (png.exists()) {
            return "media/${png.name}"
        }
        return null
    }

    private fun toRelativeMediaPath(url: String, idFallback: String): String? {
        val fileName = url.substringAfterLast('/')
        val sourceRoot = sourceDir.get().asFile
        val mediaDir = File(sourceRoot, "media")
        val localFile = File(mediaDir, fileName)
        if (localFile.exists()) {
            return "media/${localFile.name}"
        }
        // Some entries only store an id without extension
        val fallbackGif = File(mediaDir, "$idFallback.gif")
        if (fallbackGif.exists()) {
            return "media/${fallbackGif.name}"
        }
        return null
    }

    private fun normalize(input: String): String {
        return input.lowercase()
            .replace("&", " and ")
            .replace("/", " ")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun checksum(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}

@Serializable
private data class RawExercise(
    val id: String? = null,
    val exerciseId: String? = null,
    val name: String,
    val gifUrl: String? = null,
    val image: String? = null,
    val imageUrls: List<String>? = null,
    val alias: List<String>? = null,
    val tips: List<String>? = null,
    val instructions: List<String> = emptyList(),
    @SerialName("secondaryInstructions")
    val secondaryInstructions: List<String>? = null,
    val targetMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val bodyParts: List<String> = emptyList(),
    val equipments: List<String> = emptyList(),
    val force: String? = null,
    val mechanic: String? = null,
    val difficulty: String? = null,
    val category: String? = null,
)

@Serializable
data class ExerciseIndexEntry(
    val id: String,
    val name: String,
    val heroAsset: String?,
    val mediaAssets: List<String>,
    val instructions: List<String>,
    val tips: List<String>,
    val bodyParts: List<String>,
    val targetMuscles: List<String>,
    val secondaryMuscles: List<String>,
    val equipments: List<String>,
    val force: String?,
    val mechanic: String?,
    val difficulty: String?,
    val category: String?,
    val alias: List<String>,
    val normalizedName: String,
    val searchTerms: List<String>,
    val checksum: String,
)

@Serializable
data class ExerciseMetadata(
    val count: Int,
    val bodyParts: List<String>,
    val equipments: List<String>,
    val targetMuscles: List<String>,
    val secondaryMuscles: List<String>,
)
