/*

Copyright (c) 2010, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE -
                    ENERGY DEVELOPMENT UNIT (IT UNIT)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the DEPARTMENT OF ENERGY AND CLIMATE CHANGE nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

$Id$

*/
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
