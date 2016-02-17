package cc.redpen.intellij

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.ProjectManager
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

open class SettingsManager : SearchableConfigurable {
    internal var provider = RedPenProvider.instance
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

    override fun createComponent(): JComponent? {
        return settingsPane.pane
    }

    override fun isModified(): Boolean {
        return true
    }

    override fun apply() {
        provider.activeConfig = settingsPane.config
        provider.autodetect = settingsPane.autodetectLanguage.isSelected
        settingsPane.save()
        restartInspections()
    }

    override fun reset() {
        settingsPane.resetChanges()
    }

    override fun disposeUIResources() {
    }

    open fun restartInspections() {
        ApplicationManager.getApplication().invokeLater {
            val projects = ProjectManager.getInstance().openProjects
            for (project in projects) {
                if (project.isInitialized && project.isOpen && !project.isDefault) {
                    DaemonCodeAnalyzer.getInstance(project).restart()
                }
            }
        }
    }
}
