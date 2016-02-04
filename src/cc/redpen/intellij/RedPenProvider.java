package cc.redpen.intellij;

import com.intellij.codeInspection.InspectionToolProvider;

public class RedPenProvider implements InspectionToolProvider {
  public Class[] getInspectionClasses() {
    return new Class[] { RedPenInspection.class};
  }
}
