package net.foxopen.fox.module.fieldset.transformer.html;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.dom.DOMSplicer;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.track.Track;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default implementation of the HTMLValueTransformer, providing legacy functionality.
 */
public class DefaultHTMLValueTransformer
implements HTMLValueTransformer {

  private static final DefaultHTMLValueTransformer INSTANCE = new DefaultHTMLValueTransformer();

  public static DefaultHTMLValueTransformer instance() {
    return INSTANCE;
  }

  private DefaultHTMLValueTransformer() {}

  @Override
  public DOM transformPostedString(String pPostedString, HTMLTransformConfig pTransformConfig) {

    DOM lFragmentDOM = parseSubmittedValue(pPostedString, pTransformConfig);
    if(lFragmentDOM == null) {
      // When field value will not cast to xml
      // TODO PN needs testing
      String msg = "The HTML content is not well-formed. Check all your HTML tags."; //+e.getMessage();
      lFragmentDOM = DOM.createDocument("FRAGMENT").setText(pPostedString);

      lFragmentDOM.addElem("fox-error").addElem("msg").setText(msg);
    }

    return lFragmentDOM;
  }

  static DOM parseSubmittedValue(String pResponseValue, HTMLTransformConfig pTransformConfig) {

    String lXMLString = pResponseValue;

    //NBSP to space (NBSP can't be stored as XML)
    lXMLString = lXMLString.replaceAll("(?i)&nbsp;", " ");
    //Double spaces to single space
    lXMLString = lXMLString.replace("  ", " ");

    //Clean up legacy MS Word markup if required
    if(pTransformConfig.isMSWordCleanseEnabled()) {
      lXMLString = cleanWordMarkup(lXMLString);
    }

    lXMLString = "<fragment>" + lXMLString + "</fragment>";

    DOM lParsedXML;
    try {
      lParsedXML = DOM.createDocumentFromXMLString(lXMLString, false);
    }
    catch (Throwable th) {
      Track.logAlertText("HTMLWidgetParseFailed", "Failed to parse XML: " + lXMLString + "\n\nReason: " + th.getMessage());
      return null;
    }

    if(pTransformConfig.isMailMergeTranslationEnabled()) {
      replaceMailMergeMarkup(lParsedXML);
    }

    // Traverse DOM to translate and clean up unexpected nodes
    translateResponseDOM(lParsedXML, pTransformConfig);

    return lParsedXML;
  }

  private static String cleanWordMarkup(String pXMLString) {
    // Clean MS Word markup if passed through
    // Adapted from http://tim.mackey.ie/CleanWordHTMLUsingRegularExpressions.aspx
    String lXMLString = pXMLString;
    lXMLString = lXMLString.replaceAll("<[/]?(country-region|place|font|span|xml|del|ins|[ovwxp]:\\w+)[^>]*?>","");
    Matcher lMatcher = Pattern.compile("<([^>]*)(?:class|lang|style|size|face|[ovwxp]:\\w+)=(?:'[^']*'|\"[^\"]*\"|[^\\s>]+)([^>]*)>").matcher(lXMLString);
    while (lMatcher.find()) {
      lXMLString = lXMLString.replaceAll("<([^>]*)(?:class|lang|style|size|face|[ovwxp]:\\w+)=(?:'[^']*'|\"[^\"]*\"|[^\\s>]+)([^>]*)>","<$1$2>");
      lMatcher.reset(lXMLString);
    }

    return lXMLString;
  }

  private static void replaceMailMergeMarkup(DOM pParsedXML) {
    // Create node splicer on text nodes only
    DOMSplicer lDOMSplicer;
    try {
      lDOMSplicer = new DOMSplicer(pParsedXML.xpathUL("//text()", null));
    }
    catch (ExBadPath e) {
      throw e.toUnexpected();
    }

    // Use splicer to tease out mail merge fields delimited by text [[field]]
    DOMSplicer.Index lIndex, lStart, lEnd, lFalseStart;
    lIndex = lDOMSplicer.startIndex();
    SEEK_MM_LOOP:
    while((lStart=lDOMSplicer.indexOf('[', lIndex)) != null) {

      // Skip when not two "[[" characters together
      if(lDOMSplicer.charAt(lStart.incClone(1))!='[') {
        lIndex = lStart.incClone(1);
        continue SEEK_MM_LOOP;
      }

      // Skip when not two following "]]" characcters together (deals with false ends)
      lEnd = lDOMSplicer.indexOf(']', lStart.incClone(2));
      if(lEnd==null || lEnd.incClone(1).EOF() ||  lDOMSplicer.charAt(lEnd.incClone(1))!=']') {
        lIndex = lStart.incClone(1);
        continue SEEK_MM_LOOP;
      }

      // Skip when a false start "[" character exists before the "]]" end
      lFalseStart = lDOMSplicer.indexOf('[', lStart.incClone(2));
      if(lFalseStart!= null && lFalseStart.lessThan(lEnd)) {
        lIndex = lStart.incClone(1);
        continue SEEK_MM_LOOP;
      }

      // Splice in MM element over text node range
      String lFieldValue = lDOMSplicer.substring(lStart.incClone(2), lEnd);
      DOM lFieldDOM = DOM.createUnconnectedElement("MM").setText(lFieldValue);
      lDOMSplicer.replace(lStart, lEnd.incClone(2), lFieldDOM);

      // Repeat starting off where last "[[" was found as text node may have two fields in it
      lIndex = lStart;
    }
  }

  private static final void translateResponseDOM(DOM pCurrentDOM, HTMLTransformConfig pTransformConfig) {

    // Delete comments and PI nodes
    if(pCurrentDOM.isProcessingInstruction() || pCurrentDOM.isComment()) {
      pCurrentDOM.remove();
      return;
    }

    // Skip non-element nodes (e.g. text nodes)
    if(!pCurrentDOM.isElement()) {
      return;
    }

    // Process child elements first
    for(DOM lChildDOM : pCurrentDOM.getChildNodes()) {
      translateResponseDOM(lChildDOM, pTransformConfig);
    }

    // Get current element name
    String lTagName = pCurrentDOM.getName().toLowerCase();
    boolean lNameChanged = false;

    //Translate tag names
    for (TagTranslation lTranslation : pTransformConfig.getTagTranslations()) {
      if (lTranslation.requiresTranslation(lTagName)) {
        pCurrentDOM.rename(lTranslation.getTargetTagName());
        lTagName = pCurrentDOM.getName();
        lNameChanged = true;
      }
    }

    //Remove empty or whitespace only tags (note subtlety: whitespace should be moved out of the otherwise empty tag)
    if (pCurrentDOM.getChildElements().getLength() == 0 && pCurrentDOM.value(false).trim().length() == 0 && pTransformConfig.removeTagIfEmpty(lTagName)) {
      moveContentsToSiblingAndRemove(pCurrentDOM);
      return;
    }

    //Unnest nested child elements of the same name if this one is empty (i.e. to fix <p><p>..</p></p>)
    if (pTransformConfig.unnestFromEmptyParent(lTagName) && pCurrentDOM.getUL(lTagName).getLength() > 0 && pCurrentDOM.value(false).trim().length() == 0) {
      moveContentsToSiblingAndRemove(pCurrentDOM);
      return;
    }

    //If the tag is not allowed, extract its contents and remove the tag
    //Note - it is possible to have a translation which results in a disallowed tag name, but this might be required, so don't remove translated tags
    if (!lNameChanged && !pTransformConfig.tagAllowed(lTagName)) {
      moveContentsToSiblingAndRemove(pCurrentDOM);
      return;
    }

    //Remove disallowed attributes
    for (String lAttrName : pCurrentDOM.getAttrNames()) {
      if (!pTransformConfig.attributeAllowed(lTagName, lAttrName)) {
        pCurrentDOM.removeAttr(lAttrName);
      }
    }

    //Legacy fix for "align" attribute in p tag
    if("p".equals(lTagName) && pTransformConfig.attributeAllowed(lTagName, "align")) {
      String lAlign = pCurrentDOM.getAttr("align");
      if(!"left".equals(lAlign) && !"right".equals(lAlign) && !"center".equals(lAlign) && !"justify".equals(lAlign)) {
        pCurrentDOM.setAttr("align", "justify"); // Asserts case and spacing for use in doc generation
      }
    }
  }

  /** Used internally to move nested nodes out to the sibling level **/
  private static final void moveContentsToSiblingAndRemove(DOM pCurrentDOM) {

    DOM lParentDOM = pCurrentDOM.getParentOrNull();

    // Skip root element
    if(lParentDOM==null) {
      return;
    }

    // Move child to siblings
    DOMList lChildDOMList = pCurrentDOM.getChildNodes();
    DOM lChildDOM;
    while((lChildDOM = lChildDOMList.popHead()) != null) {
      lChildDOM.moveToParentBefore(lParentDOM, pCurrentDOM);
    }

    // Remove unknown node
    pCurrentDOM.remove();
  }
}
