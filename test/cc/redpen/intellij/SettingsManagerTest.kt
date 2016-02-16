package cc.redpen.intellij

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.Mockito.doNothing

class SettingsManagerTest : BaseTest() {
    internal var manager = spy(SettingsManager())
    val config = config("en")

    @Before
    fun setUp() {
        doNothing().`when`(manager).restartInspections()
        manager.provider = Mockito.mock(RedPenProvider::class.java, RETURNS_DEEP_STUBS)
        manager.settingsPane = mock()
        manager.settingsPane.autodetectLanguage = mock()
        whenever(manager.settingsPane.config).thenReturn(config);
    }

    @Test
    fun applyConfigSwitch() {
        manager.apply()
        verify(manager.provider).activeConfig = config;
    }

    @Test
    fun applyAutodetectIfNeeded() {
        whenever(manager.settingsPane.autodetectLanguage.isSelected).thenReturn(false)
        manager.apply()
        verify(manager.provider).isAutodetect = false;
    }

    @Test
    fun doNotApplyAutodetectIfNotNeeded() {
        whenever(manager.settingsPane.autodetectLanguage.isSelected).thenReturn(true)
        manager.apply()
        verify(manager.provider).isAutodetect = true;
    }

    @Test
    fun applyValidatorsAndSymbols() {
        manager.apply()
        verify(manager.settingsPane).save()
        verify(manager).restartInspections()
    }

    @Test
    fun reset() {
        manager.reset()
        verify(manager.settingsPane).resetChanges()
    }
}