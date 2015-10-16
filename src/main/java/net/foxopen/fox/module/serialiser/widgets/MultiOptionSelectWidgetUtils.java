package net.foxopen.fox.module.serialiser.widgets;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.NodeAttribute;

public class MultiOptionSelectWidgetUtils {
  private static final int DEFAULT_ITEMS_PER_ROW = 4;

  /**
   * Return the number of items per row based on the items per row node attribute. If the attribute is not specified,
   * the default number of items per row is returned.
   *
   * @param pEvalNode The multi option select node
   * @return the number of items per row
   */
  public static int getItemsPerRow(EvaluatedNode pEvalNode) {
    String lItemsPerRowAttribute = pEvalNode.getStringAttribute(NodeAttribute.ITEMS_PER_ROW);
    return !XFUtil.isNull(lItemsPerRowAttribute) ? Integer.parseInt(lItemsPerRowAttribute) : DEFAULT_ITEMS_PER_ROW;
  }
}
