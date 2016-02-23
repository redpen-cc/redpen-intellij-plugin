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
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiFile
import org.slf4j.LoggerFactory
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
        val parsers: Map<String, DocumentParser> = mapOf(
                "PLAIN_TEXT" to DocumentParser.PLAIN,
                "Markdown" to DocumentParser.MARKDOWN,
                "MultiMarkdown" to DocumentParser.MARKDOWN,
                "AsciiDoc" to DocumentParser.ASCIIDOC)

        val defaultConfigKeys = linkedSetOf("en", "ja", "ja.hankaku", "ja.zenkaku2")

        fun forProject(project: Project) = project.getComponent(RedPenProvider::class.java)!!
    }

    internal constructor(project: Project) {
        this.project = project
        this.configDir = File(project.basePath + '/' + DIRECTORY_STORE_FOLDER, "redpen")
        availableConfigKeys().forEach { loadConfig(it) }
        loadConfigKeysByFile()
    }

    /** For tests  */
    internal constructor(project: Project, configs: MutableMap<String, Configuration>) {
        this.project = project
        this.configDir = File(System.getProperty("java.io.tmpdir"))
        this.configs = configs.map { it.key to it.value.clone() }.toMap().toLinkedMap()
        this.initialConfigs = configs.map { it.key to it.value.clone() }.toMap().toLinkedMap()
    }

    internal fun loadConfig(key: String) {
        val fileName = key + ".xml"
        val loader = ConfigurationLoader()
        try {
            val file = File(configDir, fileName)

            val initialConfig = if (key in defaultConfigKeys) loader.loadFromResource("/" + fileName) else loader.load(file)
            initialConfigs[key] = initialConfig

            if (key in defaultConfigKeys && file.exists()) {
                val config = loader.load(file)
                configs[key] = config
            } else {
                configs[key] = initialConfig.clone()
            }
        }
        catch (e: Exception) {
            LoggerFactory.getLogger(javaClass).warn("Failed to load " + fileName, e)
        }
    }

    internal fun loadConfigKeysByFile() {
        val file = File(configDir, "files.xml")
        if (file.exists()) FileInputStream(file).use { configKeysByFile.loadFromXML(it) }
    }

    override fun save() {
        configDir.mkdirs()
        configs.values.forEach { c ->
            val file = File(configDir, c.key + ".xml")
            if (c.key in defaultConfigKeys && c == initialConfigs[c.key]) file.delete()
            else FileOutputStream(file).use { out -> ConfigurationExporter().export(c, out) }
        }

        val file = File(configDir, "files.xml")
        if (configKeysByFile.isEmpty) file.delete()
        else FileOutputStream(file).use { out -> configKeysByFile.storeToXML(out, null) }

        if (configDir.list().isEmpty()) configDir.delete()
    }

    internal fun availableConfigKeys() = if (!configDir.exists()) defaultConfigKeys
                                         else defaultConfigKeys + configDir.list().filter { it != "files.xml" }.map { it.replace(".xml", "") }

    infix operator fun plusAssign(config: Configuration) {
        initialConfigs[config.key] = config.clone()
        configs[config.key] = config.clone()
    }

    open fun getRedPen(): RedPen = RedPen(configs[configKey])

    open fun getRedPenFor(file: PsiFile): RedPen {
        configKey = getConfigKeyFor(file)
        return getRedPen()
    }

    open fun getConfigKeyFor(file: PsiFile) = configKeysByFile.getProperty(relativePath(file)) ?: LanguageDetector().detectLanguage(file.text)

    open fun getParser(file: PsiFile): DocumentParser? {
        return parsers[file.fileType.name]
    }

    open var activeConfig: Configuration
        get() = configs[configKey]!!
        set(config) {
            configKey = config.key
        }

    open fun setConfig(file: PsiFile, config: Configuration) {
        activeConfig = config
        configKeysByFile[relativePath(file)] = config.key
    }

    internal fun relativePath(file: PsiFile) = FileUtil.getRelativePath(project.basePath!!, file.virtualFile.path, File.separatorChar)
}
