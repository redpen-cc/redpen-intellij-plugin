package cc.redpen.intellij

import cc.redpen.config.ValidatorConfiguration
import com.intellij.openapi.project.ProjectManager
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.Mockito.doNothing
import java.util.*

class SettingsManagerTest : BaseTest() {
    val config = config("en")
    val manager: SettingsManager

    init {
        val projectManager = mock<ProjectManager>(RETURNS_DEEP_STUBS)
        whenever(application.getComponent(ProjectManager::class.java)).thenReturn(projectManager)
        manager = spy(SettingsManager(project))
    }

    @Before
    fun setUp() {
        doNothing().whenever(manager).restartInspections()
        manager.provider = mock(RETURNS_DEEP_STUBS)
        manager.settingsPane = mock()
        whenever(manager.settingsPane.config).thenReturn(config);
    }

    @Test
    fun applyConfigSwitch() {
        manager.apply()
        verify(manager.provider).activeConfig = config;
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

    @Test
    fun isNotModified() {
        val configs = manager.provider.configs
        whenever(manager.settingsPane.configs).thenReturn(configs)
        assertFalse(manager.isModified)
        verify(manager.settingsPane).applyChanges()
    }

    @Test
    fun isModified() {
        whenever(manager.settingsPane.configs).thenReturn(hashMapOf())
        assertTrue(manager.isModified)
        verify(manager.settingsPane).applyChanges()
    }

    @Test
    fun isModified_validatorProperty() {
        val config = configWithValidators(listOf(ValidatorConfiguration("blah")))
        val configs = hashMapOf("en" to config.clone())
        configs["en"]!!.validatorConfigs[0].properties["blah"] = "blah";
        whenever(manager.settingsPane.configs).thenReturn(configs)
        whenever(manager.provider.configs).thenReturn(hashMapOf("en" to config))
        assertTrue(manager.isModified)
        verify(manager.settingsPane).applyChanges()
    }
}