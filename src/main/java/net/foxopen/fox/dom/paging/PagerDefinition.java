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
