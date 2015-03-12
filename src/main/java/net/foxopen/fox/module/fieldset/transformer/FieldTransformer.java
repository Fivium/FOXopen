package net.foxopen.fox.module.fieldset.transformer;

import net.foxopen.fox.dom.DOM;

/**
 * FieldTransformers are used to transform single DOM values into Strings for sending to the user, and for applying the inverse
 * transformation to the returned value posted by the user.
 */
public interface FieldTransformer { 
  
  /**
   * Transforms the data in the given source element before it is sent to the HTML form.   
   * @param pSourceElement Element containing value to transform.
   * @return Transformed element as string.
   */
  public String applyOutboundTransform(DOM pSourceElement);
  
  /**
   * Transforms the value posted from the form into a value which can be applied to the underlying DOM.
   * @param pPostedValue Raw posted value.
   * @return String to place in DOM.
   */
  public String applyInboundTransform(String pPostedValue);
}
