package cc.redpen.intellij;

import com.intellij.codeInspection.InspectionToolProvider;

public class RedPenInspectionProvider implements InspectionToolProvider {
  public Class[] getInspectionClasses() {
    return new Class[] { RedPenInspection.class};
  }
}
