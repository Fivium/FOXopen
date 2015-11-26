package net.foxopen.fox.module;

import net.foxopen.fox.dom.DOM;


/**
 * Fox Module template model
 */
public class Template {
  /** The name of the template. */
  private final String mTemplateName;
  /** The XML Element that represents this template. */
  private final DOM mTemplateElement;

  /**
  * Constructs a template from the specified XML Dom element.
  *
  * @param templateElement the Fox Module XML element that represents the
  *        template.
  */
  public Template(DOM pTemplateElement) {
    mTemplateElement = pTemplateElement.createDocument();
    mTemplateElement.assignAllRefs();
    mTemplateName = mTemplateElement.getAttr("name");
  }

  /**
  *  Returns the name of the template.
  *
  * @return  the name of the template.
  */
  public String getName() {
    return mTemplateName;
  }

  /**
  * Returns the template element associated with this template model.
  *
  * @return the Fox Module XML element used to construct this template.
  */
  public DOM getTemplateElement() {
    return mTemplateElement;
  }
}
