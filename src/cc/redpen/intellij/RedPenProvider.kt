package cc.redpen.intellij

import cc.redpen.RedPen
import cc.redpen.config.Configuration
import cc.redpen.config.ConfigurationExporter
import cc.redpen.config.ConfigurationLoader
import cc.redpen.parser.DocumentParser
import cc.redpen.util.LanguageDetector
import com.intellij.openapi.components.SettingsSavingComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.Project.DIRECTORY_STORE_FOLDER
import com.intellij.psi.PsiFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

open class RedPenProvider : SettingsSavingComponent {
    val project: Project
    var configDir: File

    open var initialConfigs : MutableMap<String, Configuration> = LinkedHashMap()
    open var configs : MutableMap<String, Configuration> = LinkedHashMap()
    private var configKey = "en"
    internal var configKeysByFile = Properties()

    companion object {
        @JvmStatic
        val parsers: Map<String, DocumentParser> = mapOf(
                "PLAIN_TEXT" to DocumentParser.PLAIN,
                "Markdown" to DocumentParser.MARKDOWN,
                "AsciiDoc" to DocumentParser.ASCIIDOC)

        @JvmStatic
        fun forProject(project: Project) = project.getComponent(RedPenProvider::class.java)!!
    }

    internal constructor(project: Project) {
        this.project = project
        this.configDir = File(project.basePath + '/' + DIRECTORY_STORE_FOLDER, "redpen")
        listOf("en.xml", "ja.xml", "ja.hankaku.xml", "ja.zenkaku2.xml").forEach { loadConfig(it) }
        loadConfigKeysByFile()
    }

    /** For tests  */
    internal constructor(project: Project, configs: MutableMap<String, Configuration>) {
        this.project = project
        this.configDir = File(System.getProperty("java.io.tmpdir"))
        this.configs = configs
        this.initialConfigs = LinkedHashMap(configs)
    }

    internal fun loadConfig(fileName: String) {
        val loader = ConfigurationLoader()

        val initialConfig = loader.loadFromResource("/" + fileName)
        initialConfigs[initialConfig.key] = initialConfig

        val file = File(configDir, fileName)
        if (file.exists()) {
            val config = loader.load(file)
            configs[config.key] = config
        } else {
            configs[initialConfig.key] = initialConfig.clone()
        }
    }

    internal fun loadConfigKeysByFile() {
        val file = File(configDir, "files.xml")
        if (file.exists()) FileInputStream(file).use { configKeysByFile.loadFromXML(it) }
    }

    override fun save() {
        configDir.mkdirs()
        configs.values.forEach { c ->
            FileOutputStream(File(configDir, c.key + ".xml")).use { out -> ConfigurationExporter().export(c, out) }
        }
        FileOutputStream(File(configDir, "files.xml")).use { out -> configKeysByFile.storeToXML(out, null) }
    }

    infix operator fun plusAssign(config: Configuration) {
        initialConfigs[config.key] = config.clone()
        configs[config.key] = config.clone()
    }

    open fun getRedPen(): RedPen = RedPen(configs[configKey])

    open fun getRedPenFor(file: PsiFile): RedPen {
        configKey = getConfigKeyFor(file)
        return getRedPen()
    }

    open fun getConfigKeyFor(file: PsiFile) = configKeysByFile.getProperty(file.virtualFile.path) ?: LanguageDetector().detectLanguage(file.text)

    open fun getParser(file: PsiFile): DocumentParser? {
        return parsers[file.fileType.name]
    }

    fun getInitialConfig(key: String): Configuration? {
        return initialConfigs[key]
    }

    fun getConfig(key: String): Configuration? {
        return configs[key]
    }

    open var activeConfig: Configuration
        get() = configs[configKey]!!
        set(config) {
            configKey = config.key
        }

    open fun setConfig(file: PsiFile, config: Configuration) {
        activeConfig = config
        configKeysByFile[file.virtualFile.path] = config.key
    }
}
