package cc.redpen.intellij

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.openapi.wm.impl.status.TextPanel
import com.intellij.psi.PsiManager

open class StatusWidget constructor(project: Project) : EditorBasedWidget(project), CustomStatusBarWidget {
    internal var provider = RedPenProvider.instance
    private val component = TextPanel.ExtraSize()

    override fun ID(): String {
        return "RedPen"
    }

    override fun getPresentation(platformType: StatusBarWidget.PlatformType): StatusBarWidget.WidgetPresentation? {
        return null
    }

    open fun update(configKey: String) {
        component.text = "RedPen: " + configKey
    }

    override fun selectionChanged(event: FileEditorManagerEvent) {
        if (ApplicationManager.getApplication().isUnitTestMode || project == null || event.newFile == null) return

        val file = PsiManager.getInstance(project!!).findFile(event.newFile!!)
        if (file == null || provider.getParser(file) == null) return
        update(provider.getConfigKeyFor(file.text))
    }

    override fun getComponent(): TextPanel {
        return component
    }
}
