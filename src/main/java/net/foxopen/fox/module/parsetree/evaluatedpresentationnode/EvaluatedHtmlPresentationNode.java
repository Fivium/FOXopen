package net.foxopen.fox.module.parsetree.evaluatedpresentationnode;

import com.google.common.collect.Table;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.xpath.XPathResult;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.PresentationAttribute;
import net.foxopen.fox.module.evaluatedattributeresult.FixedStringAttributeResult;
import net.foxopen.fox.module.evaluatedattributeresult.PresentationStringAttributeResult;
import net.foxopen.fox.module.evaluatedattributeresult.StringAttributeResult;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.presentationnode.HtmlPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackFlag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores the content of a HTML element for serialisation
 */
public class EvaluatedHtmlPresentationNode
extends GenericAttributesEvaluatedPresentationNode<HtmlPresentationNode> {
  private static final String IMAGE_TAG_NAME = "img";
  private static final String SCRIPT_TAG_NAME = "script";
  private static final String LINK_TAG_NAME = "link";

  private static final List<String> IGNORE_ATTRIBUTES = new ArrayList<>(Arrays.asList(HtmlPresentationNode.FORCE_SELF_CLOSE_TAG_NAME));
  private final boolean mForceSelfCloseTag;

  private final String mTagName;

  private final Map<String, StringAttributeResult> mAttributeMap = new HashMap<>();

  public EvaluatedHtmlPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParent, HtmlPresentationNode pOriginalPresentationNode, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    super(pParent, pOriginalPresentationNode, pEvalContext);
    mTagName = pOriginalPresentationNode.getTagName();
    mForceSelfCloseTag = pOriginalPresentationNode.isForceSelfCloseTag();

    // Pre-cache attributes
    for (Table.Cell<String, String, PresentationAttribute> lAttributeCell : getNamespaceAttributes().cellSet()) {
      PresentationAttribute lPresentationAttribute = lAttributeCell.getValue();

      if (IGNORE_ATTRIBUTES.contains(lAttributeCell.getColumnKey()) || lPresentationAttribute == null) {
        // Skip attributes in the ignore list or value was null somehow
        continue;
      }

      // Pre-cache evaluated attribute if it's marked as evaluatable
      if (lPresentationAttribute.isEvaluatableAttribute()) {
        try {
          // Eval to string (XPathResult has a string object in it as well as an "escaping required" field)
          XPathResult lResult = pEvaluatedParseTree.getContextUElem().extendedConstantOrXPathResult(lPresentationAttribute.getEvalContextRuleDOM(), lPresentationAttribute.getValue());
          mAttributeMap.put(lAttributeCell.getColumnKey(), new PresentationStringAttributeResult(lResult));
        }
        catch (ExActionFailed e) {
          throw new ExInternal("Failed to evaluate XPath for attribute '" + lAttributeCell.getColumnKey() + "'", e);
        }
      }
    }

    // Rewrite src/href tags to use correct servlet prefix etc
    if (IMAGE_TAG_NAME.equals(mTagName)) {
      // Re-write image src tag so URI uses correct servlet
      StringAttributeResult lImageSource = mAttributeMap.get("src");

      // Image elements require a source, see http://www.w3.org/html/wg/drafts/html/master/#the-img-element:attr-img-src-2
      if (lImageSource == null || lImageSource.getString() == null) {
        throw new ExInternal("Image tag found without a src attribute.");
      }

      // Warn about images missing alt attributes
      StringAttributeResult lAltTag = mAttributeMap.get("alt");
      if (lAltTag == null || lAltTag.getString() == null) {
        Track.alert("MissingAttribute", "Image tag [@src='" + lImageSource + "'] found without an alt attribute. When specifying an img element always put an alt tag explaining the image contents for accessibility purposes.", TrackFlag.BAD_MARKUP);
      }

      mAttributeMap.put("src", new FixedStringAttributeResult(pEvaluatedParseTree.getStaticResourceOrFixedURI(lImageSource.getString())));
    }
    else if (SCRIPT_TAG_NAME.equals(mTagName) && mAttributeMap.get("src") != null) {
      // Re-write src for scripts
      StringAttributeResult lScriptSource = mAttributeMap.get("src");
      if (lScriptSource == null || lScriptSource.getString() == null) {
        mAttributeMap.put("src", new FixedStringAttributeResult(pEvaluatedParseTree.getStaticResourceOrFixedURI(lScriptSource.getString())));
      }
    }
    else if (LINK_TAG_NAME.equals(mTagName) && mAttributeMap.get("href") != null) {
      // Re-write href for links
      StringAttributeResult lLinkHref = mAttributeMap.get("href");
      if (lLinkHref == null || lLinkHref.getString() == null) {
        mAttributeMap.put("href", new FixedStringAttributeResult(pEvaluatedParseTree.getStaticResourceOrFixedURI(lLinkHref.getString())));
      }
    }
  }

  public String getTagName() {
    return mTagName;
  }

  public boolean isForceSelfCloseTag() {
    return mForceSelfCloseTag;
  }

  @Override
  public String toString() {
    return getOriginalNode().toString();
  }

  @Override
  public ComponentBuilderType getPageComponentType() {
    return ComponentBuilderType.HTML_TAG;
  }

  public Map<String, StringAttributeResult> getAttributeMap() {
    return Collections.unmodifiableMap(mAttributeMap);
  }
}
