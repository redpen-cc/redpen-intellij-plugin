package cc.redpen.intellij

import com.intellij.codeInspection.InspectionToolProvider

class RedPenInspectionProvider : InspectionToolProvider {
    override fun getInspectionClasses(): Array<Class<*>> {
        return arrayOf(RedPenInspection::class.java)
    }
}
