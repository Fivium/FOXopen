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
package net.foxopen.fox.dom.paging;


import net.foxopen.fox.module.Validatable;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.paging.style.PageControlStyle;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.Mod;

public class PagerDefinition
implements Validatable {

  private final String mName;
  private final String mPageSizeXPath;

  private final XDoCommandList mPreDo; // parsed contents of do tag
  private final XDoCommandList mPostDo;

  private final PageControlStyle mPageControlStyle;


  /**
   * Parses and map set definition based upon a xml definition
   */
  public PagerDefinition (DOM pPagerDfn, Mod pMod)
  throws ExInternal, ExModule, ExDoSyntax {
    mName = pPagerDfn.getAttr("name");
    try {
      if(XFUtil.isNull(mName)) {
        throw new ExModule("pagination-definition name required ", pPagerDfn );
      }

      mPageSizeXPath = pPagerDfn.get1SNoEx("fm:page-size");
      if(XFUtil.isNull(mPageSizeXPath)) {
        throw new ExModule("page-size must be declared on pagination-definition", pPagerDfn);
      }

      DOM lPreDoDOM = pPagerDfn.get1EOrNull("fm:pre-page/fm:do");
      DOM lPostDoDOM = pPagerDfn.get1EOrNull("fm:post-page/fm:do");
      mPreDo = XFUtil.isNull(lPreDoDOM) ? null : new XDoCommandList(pMod, lPreDoDOM);
      mPostDo = XFUtil.isNull(lPostDoDOM) ? null : new XDoCommandList(pMod, lPostDoDOM);

      DOM lPageControlStyleDOM = pPagerDfn.get1EOrNull("fm:page-controls");
      if(lPageControlStyleDOM != null) {
        mPageControlStyle = PageControlStyle.createFromDOM(lPageControlStyleDOM);
      }
      else {
        mPageControlStyle = null;
      }
    }
   catch (ExModule ex) {
      throw new ExModule("Can not parse pagination control definition " + mName, pPagerDfn, ex );
    }
  }

  public String getName() {
    return mName;
  }

  public String getPageSizeAttribute() {
    return mPageSizeXPath;
  }

  /**
  * Validates that the pagination control definition, and its sub-components, are valid.
  *
  * @param module the module where the component resides
  * @throws ExInternal if the component syntax is invalid.
  */
  public void validate(Mod pModule) {
    if (mPreDo != null) {
      mPreDo.validate(pModule);
    }
    if (mPostDo != null) {
      mPostDo.validate(pModule);
    }
  }

  public XDoCommandList getPrePageAction() {
    return mPreDo;
  }

  public XDoCommandList getPostPageAction() {
    return mPostDo;
  }

  /**
   * May be null.
   * @return
   */
  public PageControlStyle getPageControlStyle() {
    return mPageControlStyle;
  }

}
