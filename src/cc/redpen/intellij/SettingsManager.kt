package cc.redpen.intellij

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.ProjectManager
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

open class SettingsManager : SearchableConfigurable {
    internal var provider = RedPenProvider.forProject(ProjectManager.getInstance().openProjects[0])
    internal var settingsPane = SettingsPane(provider)

    override fun getId(): String {
        return helpTopic
    }

    override fun enableSearch(s: String): Runnable? {
        return null
    }

    @Nls override fun getDisplayName(): String {
        return "RedPen"
    }

    override fun getHelpTopic(): String {
        return "reference.settings.ide.settings.redpen"
    }

    override fun createComponent(): JComponent {
        return settingsPane.pane
    }

    override fun isModified(): Boolean {
        settingsPane.applyChanges()
        return provider.configs != settingsPane.configs
    }

    override fun apply() {
        provider.activeConfig = settingsPane.config
        settingsPane.save()
        restartInspections()
    }

    override fun reset() {
        settingsPane.resetChanges()
    }

    override fun disposeUIResources() {
    }

    open fun restartInspections() {
        DaemonCodeAnalyzer.getInstance(provider.project).restart()
    }
}
