import CreateXcAssets.FileAction.*
import org.gradle.api.Project
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import org.json.JSONObject
import org.json.JSONArray

internal fun doCreateXcAssets(
    project: Project,
    resourcesBase: File,
    fileClassifier: (File) -> CreateXcAssets.FileAction,
    assetNameProvider: (CreateXcAssets.AssetFileContext) -> String,
) {
    val assetsDir = File(project.buildDir, "assets").apply { mkdirs() }
    val resourcesDir = File(assetsDir, "Resources.xcassets").apply { mkdir() }

    val rootContentsJson = JSONObject()

    rootContentsJson.put("info", org.json.JSONObject().apply {
        put("author", "xcode") //TODO?
        put("version", 1)
    })

    val writer = BufferedWriter(FileWriter(File(resourcesDir, "Contents.json")))

    writer.use {
        rootContentsJson.write(writer)
    }

    resourcesBase
        .walkTopDown()
        .asIterable()
        .filter { it.isFile }
        .forEach { file ->
            when (fileClassifier(file)) {
                IMAGE -> processImageResource(file, resourcesBase, resourcesDir, assetNameProvider)
                DATA -> processDataResource(file, resourcesBase, resourcesDir, assetNameProvider)
                SKIP -> {}
            }
        }

    val projectDir = project.projectDir

    val proc = ProcessBuilder(
        "xcrun",
        "actool",
        "build/assets/Resources.xcassets",
        "--compile",
        "build",
        "--platform",
        "iphoneos",
        "--minimum-deployment-target",
        "10.0"
    ).run {
        directory(projectDir)
        start()
    }

    val output = proc.inputStream.bufferedReader().use {
        it.readText()
    }
    project.logger.debug("ACTool output: \n$output")

    if (proc.waitFor() != 0) {
        throw Exception("Failed to compile xcassets")
    }
}

internal fun processImageResource(
    imageFile: File,
    resourcesBase: File,
    resourcesDir: File,
    assetFileProvider: (CreateXcAssets.AssetFileContext) -> String,
) {
    val relativePath = imageFile.toRelativeString(resourcesBase)
    val namingContext = object: CreateXcAssets.AssetFileContext {
        override val assetFile = imageFile
        override val relativePath = relativePath
        override val type: CreateXcAssets.AssetType
            get() = CreateXcAssets.AssetType.IMAGE
    }
    val name = assetFileProvider(namingContext)
    val fileName = "$name.${imageFile.extension}"
    val imageFolder = File(resourcesDir, "$name.imageset").apply { mkdir() }

    imageFile.copyTo(File(imageFolder, fileName), overwrite = true)

    val contentsJson = JSONObject().apply {
        put(
            "images",
            JSONArray(
                arrayOf(
                    JSONObject().apply {
                        put("filename", fileName)
                        put("idiom", "universal")
                        put("scale", "1x")
                    }
                )
            )
        )
        put("info", JSONObject().apply {
            put("author", "xcode")
            put("version", 1)
        })
    }

    BufferedWriter(FileWriter(File(imageFolder, "Contents.json"))).use { writer ->
        contentsJson.write(writer)
    }
}

internal fun processDataResource(
    dataFile: File,
    resourcesBase: File,
    resourcesDir: File,
    assetNameProvider: (CreateXcAssets.AssetFileContext) -> String,
) {
    val relativePath = dataFile.toRelativeString(resourcesBase)
    val namingContext = object: CreateXcAssets.AssetFileContext {
        override val assetFile = dataFile
        override val relativePath = relativePath
        override val type: CreateXcAssets.AssetType
            get() = CreateXcAssets.AssetType.DATA
    }
    val name = assetNameProvider(namingContext)
    val fileName = "$name.txt"
    val imageFolder = File(resourcesDir, "$name.dataset").apply { mkdir() }

    dataFile.copyTo(File(imageFolder, fileName), overwrite = true)

    val contentsJson = JSONObject().apply {
        put(
            "data",
            JSONArray(
                arrayOf(
                    JSONObject().apply {
                        put("compression-type", "none")
                        put("filename", fileName)
                        put("idiom", "universal")
                    }
                )
            )
        )
        put("info", JSONObject().apply {
            put("author", "xcode") //TODO?
            put("version", 1)
        })
    }

    BufferedWriter(FileWriter(File(imageFolder, "Contents.json"))).use { writer ->
        contentsJson.write(writer)
    }
}
