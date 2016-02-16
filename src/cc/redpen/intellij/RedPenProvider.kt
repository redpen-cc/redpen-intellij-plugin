package cc.redpen.intellij

import cc.redpen.RedPen
import cc.redpen.config.Configuration
import cc.redpen.config.ConfigurationLoader
import cc.redpen.parser.DocumentParser
import cc.redpen.util.LanguageDetector
import com.google.common.collect.ImmutableMap
import com.intellij.psi.PsiFile
import java.util.*

class RedPenProvider {
    private var initialConfigs : MutableMap<String, Configuration> = LinkedHashMap()
    private var configs : MutableMap<String, Configuration> = LinkedHashMap()
    private var configKey = "en"
    var isAutodetect = true

    internal var parsers: Map<String, DocumentParser> = ImmutableMap.of(
            "PLAIN_TEXT", DocumentParser.PLAIN,
            "Markdown", DocumentParser.MARKDOWN,
            "AsciiDoc", DocumentParser.ASCIIDOC)

    companion object {
        val instance : RedPenProvider by lazy { RedPenProvider() }
    }

    private constructor() {
        loadConfig("redpen-conf.xml")
        loadConfig("redpen-conf-ja.xml")
        loadConfig("redpen-conf-ja-hankaku.xml")
        loadConfig("redpen-conf-ja-zenkaku2.xml")
        reset()
    }

    fun reset() {
        initialConfigs.forEach { e -> configs.put(e.key, e.value.clone()) }
    }

    /** For tests  */
    internal constructor(configs: MutableMap<String, Configuration>) {
        this.configs = configs
        this.initialConfigs = LinkedHashMap(configs)
    }

    private fun loadConfig(fileName: String) {
        val configuration = ConfigurationLoader().loadFromResource("/" + fileName)
        initialConfigs.put(configuration.key, configuration)
    }

    fun addConfig(config: Configuration) {
        initialConfigs.put(config.key, config.clone())
        configs.put(config.key, config.clone())
    }

    val redPen: RedPen
        get() {
           return RedPen(configs[configKey])
        }

    fun getRedPenFor(text: String): RedPen {
        if (isAutodetect) configKey = LanguageDetector().detectLanguage(text)
        return redPen
    }

    fun getParser(file: PsiFile): DocumentParser? {
        return parsers[file.fileType.name]
    }

    fun getInitialConfigs(): Map<String, Configuration> {
        return initialConfigs
    }

    fun getInitialConfig(key: String): Configuration? {
        return initialConfigs[key]
    }

    fun getConfigs(): Map<String, Configuration> {
        return configs
    }

    fun getConfig(key: String): Configuration? {
        return configs[key]
    }

    var activeConfig: Configuration
        get() = configs[configKey]!!
        set(config) {
            configKey = config.key
        }
}
