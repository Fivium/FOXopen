package net.foxopen.fox.module.serialiser.components.html;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.module.fieldset.TabGroupHiddenField;
import net.foxopen.fox.module.fieldset.action.InternalActionContext;
import net.foxopen.fox.module.fieldset.action.SwitchTabAction;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedTabGroupPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedTabGroupPresentationNode.EvaluatedTabPromptPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.TabGroupPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.tabs.EvaluatedTabInfo;
import net.foxopen.fox.module.tabs.TabGroup;
import net.foxopen.fox.track.Track;

public class TabGroupComponentBuilder
extends ComponentBuilder<HTMLSerialiser, EvaluatedTabGroupPresentationNode> {

  private static final ComponentBuilder<HTMLSerialiser, EvaluatedTabGroupPresentationNode> GROUP_BUILDER_INSTANCE = new TabGroupComponentBuilder();
  private static final ComponentBuilder<HTMLSerialiser, EvaluatedTabPromptPresentationNode> PROMPT_BUILDER_INSTANCE = new TabPromptComponentBuilder();

  public static final ComponentBuilder<HTMLSerialiser, EvaluatedTabGroupPresentationNode> getGroupBuilderInstance() {
    return GROUP_BUILDER_INSTANCE;
  }

  public static final ComponentBuilder<HTMLSerialiser, EvaluatedTabPromptPresentationNode> getPromptBuilderInstance() {
    return PROMPT_BUILDER_INSTANCE;
  }

  private TabGroupComponentBuilder() {
  }

  private static void serialiseTabContentDiv(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedTabGroupPresentationNode pEvalNode, String pTabGroupKey, EvaluatedTabInfo pEvalTabInfo,String pClass, String pStyle) {
    //TODO PN should this tab class be controllable?
    pSerialiser.append("<div class=\"tab-content " + pClass + "\" style=\"" + pStyle + "\" data-tab-group=\"" + pTabGroupKey  + "\" data-tab-key=\"" + pEvalTabInfo.getTabKey() + "\">");
    EvaluatedPresentationNode lContentEvalNode = pEvalNode.getEvaluatedContentNode(pEvalTabInfo.getTabKey());
    pSerialiser.getComponentBuilder(lContentEvalNode.getPageComponentType()).buildComponent(pSerialisationContext, pSerialiser, lContentEvalNode);
    pSerialiser.append("</div>");
  }

  private static String establishTabClass(EvaluatedTabGroupPresentationNode pEvalNode) {

    String lTabClass;
    if(TabGroupPresentationNode.TAB_STYLE_CONTAINED.equals(pEvalNode.getTabStyle())) {
      lTabClass = "contained-tabs";
    }
    else {
      lTabClass = "";
    }

    //Only set the multiline style if the threshold is exceeded
    if(pEvalNode.isMultiline()) {
      lTabClass += " multiline-tabs";
    }

    //Set group container style if not standard
    if(TabGroupPresentationNode.TAB_CONTAINER_STYLE_CONTAINED.equals(pEvalNode.getTabContainerStyle())) {
      lTabClass += " contained-tab-content";
    }

    if(!XFUtil.isNull(pEvalNode.getCSSClass())) {
      lTabClass += " " + pEvalNode.getCSSClass();
    }

    return lTabClass.trim();
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedTabGroupPresentationNode pEvalNode) {

    TabGroup lTabGroup = pEvalNode.getTabGroup();

    String lTabClass = establishTabClass(pEvalNode);

    OPEN_UL: {
      pSerialiser.append("<ul class=\"tabs");
      if(!XFUtil.isNull(pEvalNode.getTabSize())) {
        pSerialiser.append(" " + pEvalNode.getTabSize() + "-tabs");
      }

      pSerialiser.append(" " + lTabClass);

      pSerialiser.append("\" data-tab-group=\"" + lTabGroup.getTabGroupKey()  + "\" >");
    }

    EvaluatedTabInfo lSelectedTabInfo = null;
    for(EvaluatedTabInfo lTabInfo : lTabGroup.getTabInfoList()) {

      String lTabLiClass = "";
      pSerialiser.append("<li");
      if(lTabGroup.isTabSelected(lTabInfo)) {
        lTabLiClass = "current-tab";
        lSelectedTabInfo = lTabInfo;
      }
      if(!lTabInfo.isEnabled()) {
        lTabLiClass += " disabled-tab";
      }

      if(lTabLiClass.length() > 0) {
        pSerialiser.append(" class=\"" + lTabLiClass.trim() + "\"");
      }

      pSerialiser.append(" data-tab-key=\"" +  lTabInfo.getTabKey() + "\">");

      EvaluatedPresentationNode lPromptEvalNode = pEvalNode.getEvaluatedPromptNode(lTabInfo.getTabKey());
      pSerialiser.getComponentBuilder(lPromptEvalNode.getPageComponentType()).buildComponent(pSerialisationContext, pSerialiser, lPromptEvalNode);

    }
    pSerialiser.append("</ul>");

    //Always serialise the selected tab
    if(lSelectedTabInfo != null) {
      serialiseTabContentDiv(pSerialisationContext, pSerialiser, pEvalNode, lTabGroup.getTabGroupKey(), lSelectedTabInfo, lTabClass, "");
    }
    else {
      Track.alert("NoSelectedTabs", "No selected tabs in tab group " + lTabGroup.getTabGroupKey());
    }

    //Serialise other tabs if this is a client side tab group
    if(pEvalNode.isClientSide()) {
      for(EvaluatedTabInfo lTabInfo : lTabGroup.getTabInfoList()) {
        if(lTabInfo != lSelectedTabInfo) {
          serialiseTabContentDiv(pSerialisationContext, pSerialiser, pEvalNode, lTabGroup.getTabGroupKey(), lTabInfo, lTabClass, "display: none;");
        }
      }

      TabGroupHiddenField lHiddenField = pEvalNode.getHiddenField();
      pSerialiser.append("<input type=\"hidden\" data-tab-group=\"" + lTabGroup.getTabGroupKey()  + "\" " +
        "name=\"" + lHiddenField.getExternalFieldName() + "\" value=\"" + lHiddenField.getSendingValue() + "\"/>");
    }
  }

  private static class TabPromptComponentBuilder
  extends ComponentBuilder<HTMLSerialiser, EvaluatedTabPromptPresentationNode> {

    @Override
    public void buildComponent(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedTabGroupPresentationNode.EvaluatedTabPromptPresentationNode pEvalNode) {

      EvaluatedTabInfo lEvalTabInfo = pEvalNode.getEvaluatedTabInfo();

      if(lEvalTabInfo.isEnabled()) {
        String lLinkJS;
        if(pEvalNode.isClientSideTab()) {
          lLinkJS = "FOXtabs.switchTab('" + pEvalNode.getTabGroup().getTabGroupKey() + "', '" + lEvalTabInfo.getTabKey() + "');";
        }
        else {
          SwitchTabAction lInternalAction = lEvalTabInfo.createSelectTabAction(pEvalNode.getTabGroup().getTabGroupKey());
          InternalActionContext lActionContext = pSerialisationContext.getFieldSet().addInternalAction(lInternalAction);
          lLinkJS = pSerialiser.getInternalActionSubmitString(lActionContext);
        }

        pSerialiser.append("<a href=\"javascript:" + lLinkJS + "\">");
        pSerialiser.append(pEvalNode.getPromptText());
        pSerialiser.append("</a>");
      }
      else {
        //Just the prompt text for disabled tabs (no link)
        pSerialiser.append(pEvalNode.getPromptText());
      }
    }
  }
}
