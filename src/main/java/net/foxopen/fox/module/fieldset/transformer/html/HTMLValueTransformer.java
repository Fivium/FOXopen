package net.foxopen.fox.module.fieldset.transformer.html;

import net.foxopen.fox.dom.DOM;

/**
 * An HTMLValueTransformer is responsible for transforming posted values from an HTML widget into a valid DOM object.
 * This interface should be extended in the event that the legacy DefaultHTMLValueTransformer proves insufficient for
 * certain use cases.
 */
public interface HTMLValueTransformer {

  /**
   * Transforms the posted XML String into a DOM, based on the rules provided by the given HTMLTransformConfig. This
   * should be used to remove invalid or empty tags/attributes, translate invalid tags into valid ones, and perform
   * any other cleanup required. This method should always return a DOM regardless of whether the posted value was valid
   * XML. In the event of invalid XML being submitted, an error wrapping node should be returned.
   * @param pPostedString The XML string sent by the user.
   * @param pTransformConfig Rules for how the string should be processed into XML.
   * @return XML representation of the posted string, possibly containing error information if the string could not be parsed.
   */
  public DOM transformPostedString(String pPostedString, HTMLTransformConfig pTransformConfig);
}
