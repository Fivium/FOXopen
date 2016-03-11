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
import net.foxopen.fox.module.serialiser.fragmentbuilder.MustacheFragmentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.tabs.EvaluatedTabInfo;
import net.foxopen.fox.module.tabs.TabGroup;
import net.foxopen.fox.track.Track;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TabGroupComponentBuilder
extends ComponentBuilder<HTMLSerialiser, EvaluatedTabGroupPresentationNode> {
  private static final String CONTROLS_MUSTACHE_TEMPLATE = "html/TabControls.mustache";
  private static final String CONTENT_MUSTACHE_TEMPLATE = "html/TabContent.mustache";

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

  private static void serialiseTabContentDiv(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedTabGroupPresentationNode pEvalNode, String pTabGroupKey, EvaluatedTabInfo pEvalTabInfo,String pClass, String pStyle, boolean pSelected) {
    Map<String, Object> lTemplateVars = new HashMap<>();
    lTemplateVars.put("KeyID", getExternalTabKeyID(pTabGroupKey, pEvalTabInfo.getTabKey()));
    lTemplateVars.put("ContentID", getExternalTabContentID(pTabGroupKey, pEvalTabInfo.getTabKey()));
    lTemplateVars.put("Class", pClass);
    lTemplateVars.put("Style", pStyle);
    lTemplateVars.put("TabGroupKey", pTabGroupKey);
    lTemplateVars.put("TabKey", pEvalTabInfo.getTabKey());
    lTemplateVars.put("Hidden", pSelected ? "false" : "true");

    HTMLSerialiser.HTMLTempSerialiser lTempSerialiser = pSerialiser.getTempSerialiser();
    EvaluatedPresentationNode lContentEvalNode = pEvalNode.getEvaluatedContentNode(pEvalTabInfo.getTabKey());
    lTempSerialiser.getComponentBuilder(lContentEvalNode.getPageComponentType()).buildComponent(pSerialisationContext, lTempSerialiser, lContentEvalNode);
    lTemplateVars.put("TabContent", lTempSerialiser.getOutput());

    MustacheFragmentBuilder.applyMapToTemplate(CONTENT_MUSTACHE_TEMPLATE, lTemplateVars, pSerialiser.getWriter());
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

    Map<String, Object> lTemplateVars = new HashMap<>();
    lTemplateVars.put("Class", pEvalNode.getTabSize() + "-tabs " + lTabClass);
    lTemplateVars.put("TabGroupKey", lTabGroup.getTabGroupKey());

    List<Map<String, String>> lTabItems = new ArrayList<>();
    EvaluatedTabInfo lSelectedTabInfo = null;
    for(EvaluatedTabInfo lTabInfo : lTabGroup.getTabInfoList()) {
      Map<String, String> lTabItem = new HashMap<>(6);

      lTabItem.put("KeyID", getExternalTabKeyID(lTabGroup.getTabGroupKey(), lTabInfo.getTabKey()));
      lTabItem.put("ContentID", getExternalTabContentID(lTabGroup.getTabGroupKey(), lTabInfo.getTabKey()));

      String lTabLiClass = "";
      if(lTabGroup.isTabSelected(lTabInfo)) {
        lTabLiClass = "current-tab";
        lSelectedTabInfo = lTabInfo;
        lTabItem.put("Selected", "true");
      }
      else {
        lTabItem.put("Selected", "false");
      }
      if(!lTabInfo.isEnabled()) {
        lTabLiClass += " disabled-tab";
      }

      lTabItem.put("Class", lTabLiClass.trim());
      lTabItem.put("TabKey", lTabInfo.getTabKey());

      HTMLSerialiser.HTMLTempSerialiser lTempSerialiser = pSerialiser.getTempSerialiser();
      EvaluatedPresentationNode lPromptEvalNode = pEvalNode.getEvaluatedPromptNode(lTabInfo.getTabKey());
      lTempSerialiser.getComponentBuilder(lPromptEvalNode.getPageComponentType()).buildComponent(pSerialisationContext, lTempSerialiser, lPromptEvalNode);
      lTabItem.put("ItemHTML", lTempSerialiser.getOutput());

      lTabItems.add(lTabItem);
    }
    lTemplateVars.put("TabItems", lTabItems);

    MustacheFragmentBuilder.applyMapToTemplate(CONTROLS_MUSTACHE_TEMPLATE, lTemplateVars, pSerialiser.getWriter());

    //Always serialise the selected tab
    if(lSelectedTabInfo != null) {
      serialiseTabContentDiv(pSerialisationContext, pSerialiser, pEvalNode, lTabGroup.getTabGroupKey(), lSelectedTabInfo, lTabClass, "", true);
    }
    else {
      Track.alert("NoSelectedTabs", "No selected tabs in tab group " + lTabGroup.getTabGroupKey());
    }

    //Serialise other tabs if this is a client side tab group
    if(pEvalNode.isClientSide()) {
      for(EvaluatedTabInfo lTabInfo : lTabGroup.getTabInfoList()) {
        if(lTabInfo != lSelectedTabInfo) {
          serialiseTabContentDiv(pSerialisationContext, pSerialiser, pEvalNode, lTabGroup.getTabGroupKey(), lTabInfo, lTabClass, "display: none;", false);
        }
      }

      TabGroupHiddenField lHiddenField = pEvalNode.getHiddenField();
      pSerialiser.append("<input type=\"hidden\" data-tab-group=\"" + lTabGroup.getTabGroupKey()  + "\" " +
        "name=\"" + lHiddenField.getExternalFieldName() + "\" value=\"" + lHiddenField.getSendingValue() + "\"/>");
    }
  }

  public static String getExternalTabKeyID(String pTabGroupKey, String pTabKey) {
    return ("tk" + pTabGroupKey + pTabKey).replace("/", "-");
  }

  public static String getExternalTabContentID(String pTabGroupKey, String pTabKey) {
    return ("tc" + pTabGroupKey + pTabKey).replace("/", "-");
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
