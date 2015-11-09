package net.foxopen.fox.module.serialiser.widgets.html;

import com.google.common.base.Joiner;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.module.MandatoryDisplayOption;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoList;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.datanode.NodeInfo;
import net.foxopen.fox.module.datanode.NodeVisibility;
import net.foxopen.fox.module.evaluatedattributeresult.StringAttributeResult;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.TempSerialiser;
import net.foxopen.fox.module.serialiser.components.html.SingleWidgetBuildHelper;
import net.foxopen.fox.module.serialiser.fragmentbuilder.MustacheFragmentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;
import net.foxopen.fox.thread.devtoolbar.DevToolbarContext;
import net.foxopen.fox.track.Track;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListWidgetBuilder extends WidgetBuilderHTMLSerialiser<EvaluatedNodeInfoList> {
  private static final String SETOUT_LIST_CLASS_NAME = "setoutList";
  private static final String NESTED_LIST_CLASS_NAME = "nestedList";
  private static final String RESPONSIVE_LIST_CLASS_NAME = "responsiveList";
  private static final String LIST_PROMPT_CLASS_NAME = "listPrompt";

  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNodeInfoList> INSTANCE = new ListWidgetBuilder();


  public static final WidgetBuilder<HTMLSerialiser, EvaluatedNodeInfoList> getInstance() {
    return INSTANCE;
  }

  private ListWidgetBuilder () {
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
    Track.pushDebug("ListWidget", pEvalNode.getDataItem().absolute());
    try {
      boolean lResponsiveList = pEvalNode.getBooleanAttribute(NodeAttribute.RESPONSIVE_LIST, false);

      // Build table
      pSerialiser.append("<table");

      if (lResponsiveList) {
        pSerialiser.append(" " + RESPONSIVE_LIST_CLASS_NAME);
      }

      List<String> lClasses = pEvalNode.getStringAttributes(NodeAttribute.CLASS, NodeAttribute.LIST_CLASS);
      List<String> lStyles = pEvalNode.getStringAttributes(NodeAttribute.STYLE, NodeAttribute.LIST_STYLE);

      lClasses.add(SETOUT_LIST_CLASS_NAME);

      // If pEvalNode is nested in another table/form it should add the nested class, if one is specified
      if (pEvalNode.isNested()) {
        lClasses.add(NESTED_LIST_CLASS_NAME); // FOX puts on a pre-defined class name when lists are nested for default styling

        lClasses.addAll(pEvalNode.getStringAttributes(NodeAttribute.NESTED_CLASS, NodeAttribute.NESTED_TABLE_CLASS, NodeAttribute.NESTED_LIST_CLASS));
        lStyles.addAll(pEvalNode.getStringAttributes(NodeAttribute.NESTED_STYLE, NodeAttribute.NESTED_TABLE_STYLE, NodeAttribute.NESTED_LIST_STYLE));
      }

      if (lClasses.size() > 0) {
        pSerialiser.append(" class=\"");
        pSerialiser.append(Joiner.on(" ").join(lClasses));
        pSerialiser.append("\"");
      }
      if (lStyles.size() > 0) {
        pSerialiser.append("style=\"");
        pSerialiser.append(Joiner.on(" ").join(lStyles));
        pSerialiser.append("\"");
      }
      pSerialiser.append(">");


      // Build header block
      pSerialiser.append("<thead><tr>");

      if (!pEvalNode.isNestedList()) {
        // Generate a prompt for the entire list if one is specified
        StringAttributeResult lListPrompt = pEvalNode.getPrompt();
        EvaluatedPresentationNode<? extends PresentationNode> lPromptBuffer = pEvalNode.getPromptBuffer();

        String lListPromptValue = null;
        if (lPromptBuffer != null) {
          TempSerialiser<String> lTempSerialiser = pSerialiser.getTempSerialiser();
          lPromptBuffer.render(pSerialisationContext, lTempSerialiser);
          lListPromptValue = lTempSerialiser.getOutput();
        }
        else if (lListPrompt != null && !XFUtil.isNull(lListPrompt.getString())) {
          lListPromptValue = pSerialiser.getSafeStringAttribute(lListPrompt);
        }

        if (lListPromptValue != null) {
          pSerialiser.append("<th colspan=\"");
          // Ensure colspan is 1 when no columns exist, as table header block will still appear for list prompt
          pSerialiser.append(String.valueOf(Math.max(pEvalNode.getColumns().size(), 1)));

          List<String> lPromptClasses = pEvalNode.getStringAttributes(NodeAttribute.PROMPT_CLASS);
          List<String> lPromptStyles = pEvalNode.getStringAttributes(NodeAttribute.PROMPT_STYLE);

          pSerialiser.append("\" class=\"" + LIST_PROMPT_CLASS_NAME);
          if (lPromptClasses.size() > 0) {
            pSerialiser.append(" ");
            pSerialiser.append(Joiner.on(" ").join(lPromptClasses));
          }
          if (lPromptStyles.size() > 0) {
            pSerialiser.append("\" style=\"");
            pSerialiser.append(Joiner.on(" ").join(lPromptStyles));
          }
          pSerialiser.append("\">");

          pSerialiser.append(lListPromptValue);

          pSerialiser.append("</th></tr><tr>");
        }
      }

      // Generate the prompts for each column in the table
      COLUMN_PROMPT_LOOP:
      for (EvaluatedNodeInfo lTitleNode : pEvalNode.getColumns()) {

        pSerialiser.append("<th scope=\"col\"");

        List<String> lCellClasses = lTitleNode.getStringAttributes(NodeAttribute.LIST_TABLE_CLASS, NodeAttribute.LIST_CELL_CLASS, NodeAttribute.CELL_CLASS, NodeAttribute.PROMPT_CLASS);
        List<String> lCellStyles = lTitleNode.getStringAttributes(NodeAttribute.LIST_TABLE_STYLE, NodeAttribute.LIST_CELL_STYLE, NodeAttribute.CELL_STYLE, NodeAttribute.PROMPT_STYLE);

        if (lCellClasses.size() > 0) {
          pSerialiser.append(" class=\"");
          pSerialiser.append(Joiner.on(" ").join(lCellClasses));
          pSerialiser.append("\"");
        }
        if (lCellStyles.size() > 0) {
          pSerialiser.append(" style=\"");
          pSerialiser.append(Joiner.on(" ").join(lCellStyles));
          pSerialiser.append("\"");
        }

        pSerialiser.append(">");

        // Output debug information if turned on
        if (pSerialisationContext.getDevToolbarContext().isFlagOn(DevToolbarContext.Flag.HTML_GEN_DEBUG)) {
          StringBuilder lItemDebugInfo = new StringBuilder();
          lItemDebugInfo.append("<p><strong>Namespaces:</strong><ol><li>");
          lItemDebugInfo.append(Joiner.on("</li><li>").join(lTitleNode.getNamespacePrecedenceList()));
          lItemDebugInfo.append("</li></ol></p>");
          lItemDebugInfo.append("<p>");
          lItemDebugInfo.append(StringEscapeUtils.escapeHtml4(lTitleNode.getIdentityInformation()));
          lItemDebugInfo.append("</p>");
          pSerialiser.addDebugInformation(lItemDebugInfo.toString());
        }

        EvaluatedPresentationNode<? extends PresentationNode> lPromptSummaryBuffer = lTitleNode.getPromptSummaryBuffer();
        if (lPromptSummaryBuffer != null) {
          TempSerialiser<String> lTempSerialiser = pSerialiser.getTempSerialiser();
          lPromptSummaryBuffer.render(pSerialisationContext, lTempSerialiser);
          pSerialiser.append(lTempSerialiser.getOutput());
        }
        else {
          pSerialiser.append(XFUtil.nvl(pSerialiser.getSafeStringAttribute(lTitleNode.getSummaryPrompt()), ""));
        }

        // Mandatory/Optional display
        Map<String, Object> lMandatoryOptionalTemplateVars = new HashMap<>();
        lMandatoryOptionalTemplateVars.put("MandatoryClass", lTitleNode.getStringAttribute(NodeAttribute.MANDATORY_CLASS));
        lMandatoryOptionalTemplateVars.put("MandatoryStyle", lTitleNode.getStringAttribute(NodeAttribute.MANDATORY_STYLE));
        if (lTitleNode.isMandatory() && !(MandatoryDisplayOption.OPTIONAL == lTitleNode.getMandatoryDisplay() || MandatoryDisplayOption.NONE == lTitleNode.getMandatoryDisplay())) {
          // If the node is mandatory and it's not meant to be showing optional/no mandatory hints
          lMandatoryOptionalTemplateVars.put("Mandatory", lTitleNode.getStringAttribute(NodeAttribute.MANDATORY_TEXT, "*"));
        }
        else if (!lTitleNode.isMandatory() && !(MandatoryDisplayOption.MANDATORY == lTitleNode.getMandatoryDisplay() || MandatoryDisplayOption.NONE == lTitleNode.getMandatoryDisplay())) {
          // If the node is optional and it's not meant to be showing mandatory/no mandatory hints
          lMandatoryOptionalTemplateVars.put("Optional", lTitleNode.getStringAttribute(NodeAttribute.OPTIONAL_TEXT, "optional"));
        }
        MustacheFragmentBuilder.applyMapToTemplate("html/MandatoryOptional.mustache", lMandatoryOptionalTemplateVars, pSerialiser.getWriter());

        // Hint display
        if (lTitleNode.hasHint() && lTitleNode.getWidgetBuilderType() != WidgetBuilderType.BUTTON) {
          pSerialiser.addHint(pSerialisationContext, lTitleNode.getHint());
        }

        pSerialiser.append("</th>");
      }

      pSerialiser.append("</tr></thead>");


      // Generate rows with column data for all the data in the list
      ROW_LOOP:
      for (EvaluatedNodeInfo lRowNode : pEvalNode.getChildren()){
        pSerialiser.append("<tr");
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

        Map<NodeInfo, EvaluatedNodeInfo> lColumns = lRowNode.getChildrenMap();
        COLUMN_DATA_LOOP:
        for (EvaluatedNodeInfo lTitleNode : pEvalNode.getColumns()) {
          // Get the associated EvaluatedNodeInfo for the column from the row nodes child map
          EvaluatedNodeInfo lItemNode = lColumns.get(lTitleNode.getNodeInfo());
          // Output empty column if no column was found or its denied visibility
          if (lItemNode == null || lItemNode.getVisibility() == NodeVisibility.DENIED) {
            pSerialiser.append("<td></td>");
          }
          else {
            pSerialiser.append("<td");
            // Add data-title attributes for responsive lists
            if (lResponsiveList) {
              pSerialiser.append(" data-title=\"");
              // TODO - NP - This doesn't bring column-hints in, perhaps have a responsivePrompt attr on elements to get simplified prompt?
              pSerialiser.append(pSerialiser.getSafeStringAttribute(lItemNode.getSummaryPrompt()));
              pSerialiser.append("\"");
            }

            //Merge user defined cell classes with internal classes
            Collection<String> lClassList = lItemNode.getStringAttributes(NodeAttribute.LIST_TABLE_CLASS, NodeAttribute.LIST_CELL_CLASS, NodeAttribute.FIELD_CELL_CLASS, NodeAttribute.CELL_CLASS);
            lClassList.addAll(lItemNode.getCellInternalClasses());

            lClassList.addAll(SingleWidgetBuildHelper.getClassesForNodeFeatures(lItemNode));

            String lCellClasses = Joiner.on(" ").join(lClassList);
            if (!XFUtil.isNull(lCellClasses)) {
              pSerialiser.append(" class=\"" + lCellClasses + "\"");
            }

            String lCellStyles = Joiner.on(" ").join(lItemNode.getStringAttributes(NodeAttribute.LIST_TABLE_STYLE, NodeAttribute.LIST_CELL_STYLE, NodeAttribute.FIELD_CELL_STYLE, NodeAttribute.CELL_STYLE));
            if (!XFUtil.isNull(lCellStyles)) {
              pSerialiser.append(" style=\"" + lCellStyles + "\"");
            }

            HTMLSerialiser.serialiseDataAttributes(pSerialiser, lItemNode);

            pSerialiser.append(">");

            // Output debug information if turned on
            if (pSerialisationContext.getDevToolbarContext().isFlagOn(DevToolbarContext.Flag.HTML_GEN_DEBUG)) {
              SingleWidgetBuildHelper.outputDebugInformation(pSerialiser, lItemNode);
            }

            pSerialiser.getWidgetBuilder(lItemNode.getWidgetBuilderType()).buildWidget(pSerialisationContext, pSerialiser, lItemNode);

            SingleWidgetBuildHelper.insertErrorMessage(pSerialiser, lItemNode);
            SingleWidgetBuildHelper.insertHistoryMessage(pSerialiser, lItemNode);

            pSerialiser.append("</td>");
          }
        } // COLUMN_DATA_LOOP

        pSerialiser.append("</tr>");
      } // ROW_LOOP

      pSerialiser.append("</table>");
    }
    finally {
      Track.pop("ListWidget");
    }
  }
}
