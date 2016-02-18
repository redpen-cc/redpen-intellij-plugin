package cc.redpen.intellij

import cc.redpen.config.ConfigurationLoader
import cc.redpen.config.Symbol
import cc.redpen.config.SymbolType.AMPERSAND
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class RedPenProviderTest : BaseTest() {
    val provider = RedPenProvider.instance

    @Test
    fun allConfigFilesAreLoaded() {
        assertEquals("en", provider.getConfig("en")!!.key)
        assertEquals("ja", provider.getConfig("ja")!!.key)
        assertEquals("ja.hankaku", provider.getConfig("ja.hankaku")!!.key)
        assertEquals("ja.zenkaku2", provider.getConfig("ja.zenkaku2")!!.key)
    }

    @Test
    fun getRedPenFor_autodetectsLanguage() {
        provider.autodetect = true

        var redPen = provider.getRedPenFor("Hello")
        assertEquals("en", redPen.configuration.key)

        redPen = provider.getRedPenFor("こんにちは")
        assertEquals("ja", redPen.configuration.key)
    }

    @Test
    fun languageAutodetectionCanBeDisabled() {
        provider.autodetect = false

        val redPen = provider.getRedPenFor("こんにちは")
        assertEquals("en", redPen.configuration.key)
    }

    @Test
    fun saveAndLoad() {
        provider.configDir = File(System.getProperty("java.io.tmpdir"), "redpen-tmp-config")
        provider.getConfig("ja")!!.symbolTable.overrideSymbol(Symbol(AMPERSAND, '*'))
        provider.save()

        assertEquals(provider.getConfig("en"), ConfigurationLoader().load(File(provider.configDir, "en.xml")))
        assertEquals(provider.getConfig("ja"), ConfigurationLoader().load(File(provider.configDir, "ja.xml")))

        provider.loadConfig("ja.xml")
        assertEquals('*', provider.getConfig("ja")!!.symbolTable.getSymbol(AMPERSAND).value)
        assertFalse(provider.getInitialConfig("ja")!!.equals(provider.getConfig("ja")))
        assertTrue(provider.getInitialConfig("en")!!.equals(provider.getConfig("en")))

        provider.configDir.deleteRecursively()
    }
}