package net.foxopen.fox.module.tabs;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.DisplayOrderComparator;
import net.foxopen.fox.module.parsetree.presentationnode.ContainerPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.TabGroupPresentationNode.TabPromptPresentationNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Parsed module markup which can be evaluated with a ContextUElem to provide a list of EvaluatedTabInfo objects. Subclasses
 * represent the different type of tab markup available to a module developer.
 */
public abstract class TabInfoProvider {

  private static final String PRE_TAB_ACTION_NAME_ATTR = "preTabChangeAction";
  private static final String POST_TAB_ACTION_NAME_ATTR = "postTabChangeAction";
  private static final String DEFAULT_TAB_CONTEXT_NAME = "tab";

  /** The parsed markup for an individual tab. */
  private final TabInfo mTabInfo;

  public static List<TabInfoProvider> fromDOM(DOM pTabGroupDOM)
  throws ExModule {

    List<TabInfoProvider> lProviderList = new ArrayList<>(pTabGroupDOM.getChildElements().size());

    String lDefaultPreTabActionName = pTabGroupDOM.getAttrOrNull(PRE_TAB_ACTION_NAME_ATTR);
    String lDefaultPostTabActionName = pTabGroupDOM.getAttrOrNull(POST_TAB_ACTION_NAME_ATTR);

    for(DOM lChild : pTabGroupDOM.getChildElements()) {
      String lElemName = lChild.getLocalName();
      if("tab".equals(lElemName)) {
        lProviderList.add(new BasicTabInfoProvider(createTabInfoFromDOM(lChild, lDefaultPreTabActionName, lDefaultPostTabActionName, true)));
      }
      else if("tab-for-each-dom".equals(lElemName)) {
        lProviderList.add(new ForEachDOMTabInfoProvider(lChild, createTabInfoFromDOM(lChild, lDefaultPreTabActionName, lDefaultPostTabActionName, false)));
      }
      else if("tab-for-each-number".equals(lElemName)) {
        lProviderList.add(new ForEachNumberTabInfoProvider(lChild, createTabInfoFromDOM(lChild, lDefaultPreTabActionName, lDefaultPostTabActionName, true)));
      }
      else {
        throw new ExModule("Unrecognised element name within fm:tab-group " + lElemName);
      }
    }

    return lProviderList;
  }

  /**
   *
   * @param pDOM
   * @param pDefaultPreTabActionName
   * @param pDefaultPostTabActionName
   * @param pKeyRequired If true, a tabKey attribute must be provided. If false, the attribute must not be provided.
   * @return
   * @throws ExModule
   */
  protected static TabInfo createTabInfoFromDOM(DOM pDOM, String pDefaultPreTabActionName, String pDefaultPostTabActionName, boolean pKeyRequired)
  throws ExModule {
    String lTabKeyXPath = pDOM.getAttrOrNull("tabKey");
    String lTabEnabledXPath = pDOM.getAttrOrNull("enabled");
    String lTabContextName = XFUtil.nvl(pDOM.getAttrOrNull("tabContextName"), DEFAULT_TAB_CONTEXT_NAME);

    //Check a key has (not) been provided if required
    if(pKeyRequired && XFUtil.isNull(lTabKeyXPath)) {
      throw new ExModule("Tabs in " + pDOM.getName() + " element must specify a tab key");
    }
    else if(!pKeyRequired && !XFUtil.isNull(lTabKeyXPath)) {
      throw new ExModule("Tabs in " + pDOM.getName() + " element cannot specify a tab key");
    }

    //Establish the prompt. If the tab-prompt has a text attribute and no content we just need a TabPrompt presentation
    //node. Otherwise we need to parse the contents of the element (the user has specified a custom prompt layout).
    PresentationNode lPromptPresentationNode;
    try {
      DOM lPromptTag = pDOM.get1E("fm:tab-prompt");
      String lPromptXPath = lPromptTag.getAttrOrNull("text");

      if(XFUtil.isNull(lPromptXPath) && lPromptTag.getChildNodes().size() == 0) {
        throw new ExModule("fm:tab-prompt must provide either a text attribute or HTML buffer content");
      }
      else if(!XFUtil.isNull(lPromptXPath) && lPromptTag.getChildNodes().size() > 0) {
        throw new ExModule("fm:tab-prompt cannot provide both a text attribute and HTML buffer content");
      }

      if(!XFUtil.isNull(lPromptXPath)) {
        lPromptPresentationNode = new TabPromptPresentationNode(lPromptXPath);
      }
      else {
        lPromptPresentationNode = new ContainerPresentationNode(lPromptTag);
      }
    }
    catch (ExCardinality e) {
      throw new ExModule("Invalid markup for fm:tab-prompt element", e);
    }

    PresentationNode lContentPresentationNode;
    try {
      lContentPresentationNode = new ContainerPresentationNode(pDOM.get1E("fm:tab-content"));
    }
    catch (ExCardinality e) {
      throw new ExModule("Invalid markup for fm:tab-content element", e);
    }

    String lPreTabActionName = XFUtil.nvl(pDOM.getAttrOrNull(PRE_TAB_ACTION_NAME_ATTR), pDefaultPreTabActionName);
    String lPostTabActionName = XFUtil.nvl(pDOM.getAttrOrNull(POST_TAB_ACTION_NAME_ATTR), pDefaultPostTabActionName);

    String lDisplayOrder = XFUtil.nvl(pDOM.getAttr("displayOrder"), DisplayOrderComparator.AUTO_ATTR_VALUE);

    String lDefaultXPath = XFUtil.nvl(pDOM.getAttr("default"), "false()");

    return new TabInfo(lTabKeyXPath, lTabEnabledXPath, lTabContextName, lPreTabActionName, lPostTabActionName, lDisplayOrder, lDefaultXPath, lPromptPresentationNode, lContentPresentationNode);
  }

  protected TabInfoProvider(TabInfo pTabInfo) {
    mTabInfo = pTabInfo;
  }

  protected TabInfo getTabInfo() {
    return mTabInfo;
  }

  public abstract List<EvaluatedTabInfo> evaluate(DOM pRelativeDOM, ContextUElem pContextUElem, boolean pEvaluateDefaultAttr);

}
