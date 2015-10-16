package net.foxopen.fox.module.serialiser.widgets;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.serialiser.FOXGridUtils;

public class FormWidgetUtils {
  /**
   * Return the number of form columns based on the form columns or form max columns node attributes.
   * If neither attributes are specified, the maximum possible number of grid columns is returned.
   *
   * @param pEvalNode The form node
   * @return the number form columns
   */
  public static int getFormColumns(EvaluatedNode pEvalNode) {
    String lFormColumnsAttribute = pEvalNode.getStringAttribute(NodeAttribute.FORM_COLUMNS);

    if (XFUtil.isNull(lFormColumnsAttribute)) {
      lFormColumnsAttribute = pEvalNode.getStringAttribute(NodeAttribute.FORM_MAX_COLUMNS);
    }

    return !XFUtil.isNull(lFormColumnsAttribute) ? Integer.parseInt(lFormColumnsAttribute) : FOXGridUtils.getMaxColumns();
  }
}
