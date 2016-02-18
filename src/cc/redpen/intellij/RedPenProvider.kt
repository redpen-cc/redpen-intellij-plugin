package cc.redpen.intellij

import cc.redpen.RedPen
import cc.redpen.config.Configuration
import cc.redpen.config.ConfigurationExporter
import cc.redpen.config.ConfigurationLoader
import cc.redpen.parser.DocumentParser
import cc.redpen.util.LanguageDetector
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.SettingsSavingComponent
import com.intellij.psi.PsiFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

open class RedPenProvider : SettingsSavingComponent {
    var configDir = File(PathManager.getConfigPath(), "redpen")

    private var initialConfigs : MutableMap<String, Configuration> = LinkedHashMap()
    private var configs : MutableMap<String, Configuration> = LinkedHashMap()
    private var configKey = "en"
    internal var configKeysByFile = Properties()
    open var autodetect = true

    internal var parsers: Map<String, DocumentParser> = mapOf(
            "PLAIN_TEXT" to DocumentParser.PLAIN,
            "Markdown" to DocumentParser.MARKDOWN,
            "AsciiDoc" to DocumentParser.ASCIIDOC)

    companion object {
        @JvmStatic
        val instance: RedPenProvider by lazy { ApplicationManager.getApplication().getComponent(RedPenProvider::class.java) ?: RedPenProvider() }
    }

    private constructor() {
        listOf("en.xml", "ja.xml", "ja.hankaku.xml", "ja.zenkaku2.xml").forEach { loadConfig(it) }
        loadConfigKeysByFile()
    }

    /** For tests  */
    internal constructor(configs: MutableMap<String, Configuration>) {
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

    fun addConfig(config: Configuration) {
        initialConfigs.put(config.key, config.clone())
        configs.put(config.key, config.clone())
    }

    open fun getRedPen(): RedPen = RedPen(configs[configKey])

    fun getRedPenFor(file: PsiFile): RedPen {
        configKey = configKeysByFile.getProperty(file.virtualFile.path) ?: getConfigKeyFor(file.text)
        return getRedPen()
    }

    open fun getConfigKeyFor(text: String) = if (autodetect) LanguageDetector().detectLanguage(text) else configKey

    open fun getParser(file: PsiFile): DocumentParser? {
        return parsers[file.fileType.name]
    }

    fun getInitialConfigs(): Map<String, Configuration> {
        return initialConfigs
    }

    fun getInitialConfig(key: String): Configuration? {
        return initialConfigs[key]
    }

    open fun getConfigs(): Map<String, Configuration> {
        return configs
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
