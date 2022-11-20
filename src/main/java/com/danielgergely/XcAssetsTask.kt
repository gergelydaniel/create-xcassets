import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class CreateXcAssets : DefaultTask() {

    private var _resourcesDir: File? = null
    private var _fileClassifier: ((File) -> FileAction)? = null
    private var assetNameProvider: ((AssetFileContext) -> String) = {
        it.assetFile.name
    }

    @get:InputDirectory
    var resourcesDirectory: File
        get() = _resourcesDir ?: throw IllegalStateException("resourcesDirectory is not assigned")
        set(value) {
            _resourcesDir = value
        }

    @get:OutputFile
    val carFile: File = File(project.buildDir, "Assets.car")

    fun fileFilter(fileClassifier: (File) -> FileAction) {
        _fileClassifier = fileClassifier
    }

    fun assetName(provider: AssetFileContext.() -> String) {
        assetNameProvider = provider
    }

    @TaskAction
    fun createXcAssets() {
        doCreateXcAssets(
            project = project,
            resourcesBase = resourcesDirectory,
            fileClassifier = checkFileClassifier(),
            assetNameProvider = assetNameProvider,
        )
    }

    private fun checkFileClassifier(): ((File) -> FileAction) {
        return _fileClassifier ?: throw IllegalStateException(
            """
                CreateXcAssets: file filter hasn't been specified.
                
                You must implement a file filter. Example:
                fileFilter { file ->
                    when {
                        file.extension.equals("png", true) -> IMAGE
                        else -> DATA
                    }
                }
            """.trimIndent()
        )
    }

    enum class FileAction {
        DATA,
        IMAGE,
        SKIP,
    }

    enum class AssetType {
        DATA,
        IMAGE
    }

    interface AssetFileContext {
        val assetFile: File
        val relativePath: String
        val type: AssetType
    }
}
