package net.foxopen.fox.module.serialiser.widgets.html;

import com.google.common.base.Joiner;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoList;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;

import java.util.List;

public class FormListWidgetBuilder extends WidgetBuilderHTMLSerialiser<EvaluatedNodeInfoList> {
  private static final String SETOUT_LIST_CLASS_NAME = "setoutList";
  private static final String NESTED_LIST_CLASS_NAME = "nestedList";

  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNodeInfoList> INSTANCE = new FormListWidgetBuilder();

  public static final WidgetBuilder<HTMLSerialiser, EvaluatedNodeInfoList> getInstance() {
    return INSTANCE;
  }

  private FormListWidgetBuilder() {
  }

  @Override
  public boolean hasPrompt(EvaluatedNodeInfoList pEvalNode) {
    return pEvalNode.isNestedList() && pEvalNode.hasPrompt();
  }

  @Override
  public void buildWidget(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNodeInfoList pEvalNode) {
    buildWidgetInternal(pSerialisationContext, pSerialiser, pEvalNode);
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNodeInfoList pEvalNode) {
    if (pEvalNode.getChildren().size() > 0) {
      pSerialiser.append("<ul class=\"");

      // Add classes/styles
      List<String> lClasses = pEvalNode.getStringAttributes(NodeAttribute.CLASS, NodeAttribute.LIST_CLASS);
      List<String> lStyles = pEvalNode.getStringAttributes(NodeAttribute.STYLE, NodeAttribute.LIST_STYLE);

      lClasses.add(SETOUT_LIST_CLASS_NAME);

      if (pEvalNode.isNested()) {
        lClasses.add(NESTED_LIST_CLASS_NAME); // FOX puts on a pre-defined class name when lists are nested for default styling

        lClasses.addAll(pEvalNode.getStringAttributes(NodeAttribute.NESTED_CLASS, NodeAttribute.NESTED_TABLE_CLASS, NodeAttribute.NESTED_LIST_CLASS));
        lStyles.addAll(pEvalNode.getStringAttributes(NodeAttribute.NESTED_STYLE, NodeAttribute.NESTED_TABLE_STYLE, NodeAttribute.NESTED_LIST_STYLE));
      }

      if (lClasses.size() > 0) {
        pSerialiser.append(Joiner.on(" ").join(lClasses));
      }
      if (lStyles.size() > 0) {
        pSerialiser.append("\" style=\"");
        pSerialiser.append(Joiner.on(" ").join(lStyles));
      }

      pSerialiser.append("\">");

      for (EvaluatedNodeInfo lRowNode : pEvalNode.getChildren()) {
        pSerialiser.append("<li");

        List<String> lRowClassList = lRowNode.getStringAttributes(NodeAttribute.CLASS, NodeAttribute.LIST_ROW_CLASS);
        lRowClassList.addAll(lRowNode.getCellInternalClasses());
        String lRowClasses = Joiner.on(" ").join(lRowClassList);
        if (!XFUtil.isNull(lRowClasses)) {
          pSerialiser.append(" class=\"");
          pSerialiser.append(lRowClasses);
          pSerialiser.append("\"");
        }
        String lRowStyles = Joiner.on(" ").join(lRowNode.getStringAttributes(NodeAttribute.STYLE, NodeAttribute.LIST_ROW_STYLE));
        if (!XFUtil.isNull(lRowStyles)) {
          pSerialiser.append(" style=\"");
          pSerialiser.append(lRowStyles);
          pSerialiser.append("\"");
        }

        HTMLSerialiser.serialiseDataAttributes(pSerialiser, lRowNode);

        pSerialiser.append(">");

        pSerialiser.getWidgetBuilder(lRowNode.getWidgetBuilderType()).buildWidget(pSerialisationContext, pSerialiser, lRowNode);
        pSerialiser.append("</li>");
      }

      pSerialiser.append("</ul>");
    }
  }
}
