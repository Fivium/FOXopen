package net.foxopen.fox.module.serialiser.widgets.html;


import com.google.common.base.Joiner;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.datanode.NodeInfo;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.fragmentbuilder.MustacheFragmentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.Map;


// TODO - NP - Abandon hope all ye who enter...
/**
 * Horrific class that does the same as the FOX4 code did, which is shockingly bad
 */
public class ErrorRefWidgetBuilder extends WidgetBuilderHTMLSerialiser<EvaluatedNode> {
  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> INSTANCE = new ErrorRefWidgetBuilder();
  private static final String MUSTACHE_TEMPLATE = "html/LinkWidget.mustache";

  public static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> getInstance() {
    return INSTANCE;
  }

  private ErrorRefWidgetBuilder () {
  }

  @Override
  public boolean hasPrompt(EvaluatedNode pEvalNode) {
    return false;
  }

  @Override
  public void buildPrompt(HTMLSerialiser pSerialiser, EvaluatedNode pEvalNode) {
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNode pEvalNode) {

    //The ref to the item with the error is the text content of the error-ref node being set out
    String lErrorItemDOMRef = pEvalNode.getDataItem().value();

    DOM lErrorElement = pEvalNode.getContextUElem().getElemByRefOrNull(lErrorItemDOMRef);
    NodeInfo lMatchedItemNodeInfo = pSerialisationContext.getModule().getNodeInfo(lErrorElement);

    String lErrorItemNavAction = getNavActionName(lMatchedItemNodeInfo);

    Map<String, Object> lTemplateVars = super.getGenericTemplateVars(pSerialiser, pEvalNode);

    lTemplateVars.put("PromptText", getPrompt(lMatchedItemNodeInfo, pEvalNode));

    if (XFUtil.isNull(lErrorItemNavAction)) {
      lTemplateVars.put("ReadOnly", true);
      lTemplateVars.put("Class", Joiner.on(" ").skipNulls().join(lTemplateVars.get("Class"), "disabledLink"));
    }
    else {
      // TODO - NP - This should be done higher up, at pEvalNode creation perhaps?
      pSerialisationContext.getFieldSet().registerExternalRunnableAction(lErrorItemNavAction, lErrorItemDOMRef);
      lTemplateVars.put("AttributeSafeActionJS", StringEscapeUtils.escapeHtml4("FOXjs.action({ref:'" + lErrorItemNavAction + "', ctxt:'" + lErrorItemDOMRef + "'});"));
    }

    MustacheFragmentBuilder.applyMapToTemplate(MUSTACHE_TEMPLATE, lTemplateVars, pSerialiser.getWriter());
  }

  private String getPrompt(NodeInfo pNodeInfo, EvaluatedNode pEvalNode) {
    // Try the fox:prompt attribute
    String lPromptValue = pNodeInfo.getAttribute(NodeInfo.FOX_NAMESPACE, NodeAttribute.PROMPT.getExternalString());
    // If no prompt defined, try fox:prompt-short
    if (lPromptValue == null) {
      lPromptValue = pNodeInfo.getAttribute(NodeInfo.FOX_NAMESPACE, NodeAttribute.PROMPT_SHORT.getExternalString());
    }

    if (XFUtil.exists(lPromptValue)) {
      // Evaluate the prompt string or xpath and return the result if not empty
      try {
        lPromptValue = pEvalNode.getContextUElem().extendedStringOrXPathString(pEvalNode.getEvaluateContextRuleItem(), lPromptValue);
        if (XFUtil.exists(lPromptValue)) {
          return lPromptValue;
        }
      }
      catch (ExActionFailed e) {
      /* IGNORED */
      }
    }

    // Try fox:action
    lPromptValue = pNodeInfo.getAttribute(NodeInfo.FOX_NAMESPACE, NodeAttribute.ACTION.getExternalString());
    if (XFUtil.exists(lPromptValue)) {
      // Return the init capped action name (skip past state name if defined)
      int pos = lPromptValue.indexOf("/");
      if (pos != -1) {
        return XFUtil.initCap(lPromptValue.substring(pos + 1));
      }
      return XFUtil.initCap(lPromptValue);
    }

    // Resort to the element name if nothing else available
    lPromptValue = pNodeInfo.getName();
    if (XFUtil.exists(lPromptValue)) {
      return XFUtil.initCap(lPromptValue);
    }
    else {
      return null;
    }
  }

  /**
   * Attempt to get a the action name from a navAction attribute on this element or the parents
   *
   * @return Name of action to run
   */
  public String getNavActionName(NodeInfo pNodeInfo) {
    String lNavActionName = pNodeInfo.getAttribute(NodeInfo.FOX_NAMESPACE, "navAction");

    if (XFUtil.isNull(lNavActionName)) {
      NodeInfo lParent = pNodeInfo.getParentNodeInfo();
      if (lParent != null) {
        lNavActionName = getNavActionName(lParent);
      }
      else {
        return null;
      }
    }

    return lNavActionName;
  }
}
