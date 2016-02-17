package cc.redpen.intellij

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test

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
    fun reset() {
        val config = provider.getConfig("en")

        provider.reset()

        assertNotSame(config, provider.getConfig("en"))
        assertEquals("en", provider.getConfig("en")?.key)
    }
}