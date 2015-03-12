package net.foxopen.fox.module.parsetree.evaluatedpresentationnode;

import com.google.common.collect.Table;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.PresentationAttribute;
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

  private final Map<String, String> mAttributeMap = new HashMap<>();

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

      String lAttrValue = lPresentationAttribute.getValue();
      // Pre-cache evaluated attribute if it's marked as evaluatable
      if (lPresentationAttribute.isEvaluatableAttribute()) {
        try {
          lAttrValue = pEvaluatedParseTree.getContextUElem().extendedStringOrXPathString(lPresentationAttribute.getEvalContextRuleDOM(), lAttrValue);
        }
        catch (ExActionFailed e) {
          throw e.toUnexpected("Failed evaluating XPath on attribute '" + lAttributeCell.getColumnKey() + "' for a regular HTML element");
        }
      }

      mAttributeMap.put(lAttributeCell.getColumnKey(), lAttrValue);
    }

    // Rewrite src/href tags to use correct servlet prefix etc
    if (IMAGE_TAG_NAME.equals(mTagName)) {
      // Re-write image src tag so URI uses correct servlet
      String lImageSource = mAttributeMap.get("src");

      // Image elements require a source, see http://www.w3.org/html/wg/drafts/html/master/#the-img-element:attr-img-src-2
      if (lImageSource == null) {
        throw new ExInternal("Image tag found without a src attribute.");
      }

      // Warn about images missing alt attributes
      if (mAttributeMap.get("alt") == null) {
        Track.alert("MissingAttribute", "Image tag [@src='" + lImageSource + "'] found without an alt attribute. When specifying an img element always put an alt tag explaining the image contents for accessibility purposes.", TrackFlag.BAD_MARKUP);
      }

      mAttributeMap.put("src", pEvaluatedParseTree.getStaticResourceOrFixedURI(lImageSource));
    }
    else if (SCRIPT_TAG_NAME.equals(mTagName) && mAttributeMap.get("src") != null) {
      // Re-write src for scripts
      mAttributeMap.put("src", pEvaluatedParseTree.getStaticResourceOrFixedURI(mAttributeMap.get("src")));
    }
    else if (LINK_TAG_NAME.equals(mTagName) && mAttributeMap.get("href") != null) {
      // Re-write href for links
      mAttributeMap.put("href", pEvaluatedParseTree.getStaticResourceOrFixedURI(mAttributeMap.get("href")));
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

  public Map<String, String> getAttributeMap() {
    return Collections.unmodifiableMap(mAttributeMap);
  }
}
