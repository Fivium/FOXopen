package net.foxopen.fox.dom.paging;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.thread.ActionRequestContext;

/**
 * Encapsulated logic for setting up a Pager. Users may provide definitions for pagers in multiple locations (for-each,
 * set-out, run-query, etc) but the markup rules are always the same.<br/><br/>
 *
 * A user provides either a pagination definition or a page size, depending on whether they require a full-blown pagination
 * setup with custom pre/post page processing or a simple pager respectively. The page size is a stringifiable XPath which
 * requires evaluating. In addition, an invoke name must always be specified. <br/><br/>
 *
 * Consumers only need to use a PagerSetup when a Pager is potentially being constructed. To resolve a Pager after construction,
 * the invoke name and optional match ID form the lookup tuple. A PagerSetup is only required for determining the page size/
 * pager definition, which is immutable after Pager construction.
 */
public class PagerSetup {

  //Mutually exclusive
  private final String mDefinitionName;
  private final String mPageSizeAttr;
  //Not null
  private final String mInvokeName;

  /**
   * Parses an element possibly containing PagerSetup attributes, returning a new PagerSetup if the attributes are specified
   * and valid.
   * @param pDefinitionElem DOM element to parse.
   * @param pDefinitionAttrName Attribute name of the "pagination-definition" attribute.
   * @param pPageSizeAttrName Attribute name of the "page-size" attribute.
   * @param pInvokeAttrName Attribute name of the "pagination-invoke-name" attribute.
   * @return A new PagerSetup, or null if no attributes were defined.
   * @throws ExModule If the attributes are specified and invalid.
   */
  public static PagerSetup fromDOMMarkupOrNull(DOM pDefinitionElem, String pDefinitionAttrName, String pPageSizeAttrName, String pInvokeAttrName)
  throws ExModule {
    return fromDOMMarkupOrNull(pDefinitionElem, pDefinitionAttrName, pPageSizeAttrName, pInvokeAttrName, null);
  }

  /**
   * Parses an element possibly containing PagerSetup attributes, returning a new PagerSetup if the attributes are specified
   * and valid.
   * @param pDefinitionElem DOM element to parse.
   * @param pDefinitionAttrName Attribute name of the "pagination-definition" attribute.
   * @param pPageSizeAttrName Attribute name of the "page-size" attribute.
   * @param pInvokeAttrName Attribute name of the "pagination-invoke-name" attribute.
   * @param pAltDefinitionName Alternative source for a pagination-definition attribute. This is only used if the attribute
   * is not specified. Can be null.
   * @return A new PagerSetup, or null if no attributes were defined.
   * @throws ExModule If the attributes are specified and invalid.
   */
  public static PagerSetup fromDOMMarkupOrNull(DOM pDefinitionElem, String pDefinitionAttrName, String pPageSizeAttrName, String pInvokeAttrName, String pAltDefinitionName)
  throws ExModule {

    String lDefinitionName = XFUtil.nvl(pDefinitionElem.getAttrOrNull(pDefinitionAttrName), pAltDefinitionName);
    String lPageSize = pDefinitionElem.getAttrOrNull(pPageSizeAttrName);
    String lInvokeName = pDefinitionElem.getAttrOrNull(pInvokeAttrName);

    if(!XFUtil.isNull(lDefinitionName) && !XFUtil.isNull(lPageSize)) {
      throw new ExModule(pDefinitionAttrName + " and " + pPageSizeAttrName + " attributes are mutually exclusive");
    }
    else if(XFUtil.isNull(lDefinitionName) && XFUtil.isNull(lPageSize)) {
      if(XFUtil.isNull(lInvokeName)) {
        //No pagination attributes defined at all - this is OK
        return null;
      }
      else {
        throw new ExModule(pInvokeAttrName + " cannot be specified without one of " + pDefinitionAttrName + " or " + pPageSizeAttrName);
      }
    }
    else {
      if(XFUtil.isNull(lInvokeName)) {
        throw new ExModule(pInvokeAttrName + " must be specified if one of " + pDefinitionAttrName + " or " + pPageSizeAttrName + " is specified");
      }
      else {
        return new PagerSetup(lDefinitionName, lPageSize, lInvokeName);
      }
    }
  }

  private PagerSetup(String pDefinitionName, String pPageSizeAttr, String pInvokeName) {
    mDefinitionName = pDefinitionName;
    mPageSizeAttr = pPageSizeAttr;
    mInvokeName = pInvokeName;
  }

  public String getInvokeName() {
    return mInvokeName;
  }

  /**
   * Evaluates this pager setup object by executing the page size XPath and parsing the result to an int.
   * @param pRequestContext For XPath evluation.
   * @param pMatchId Optional, if required by the Pager to be created.
   * @return Evaulated copy of this PagerSetup.
   */
  public EvaluatedPagerSetup evalute(ActionRequestContext pRequestContext, String pMatchId) {

    PagerDefinition lPagerDefinition = null;
    if(!XFUtil.isNull(mDefinitionName)) {
      lPagerDefinition = pRequestContext.getCurrentModule().getPagerDefinitionByName(mDefinitionName);
    }

    //Determine the page size attribute
    String lPageSizeAttr;
    if(lPagerDefinition != null) {
      lPageSizeAttr = lPagerDefinition.getPageSizeAttribute();
    }
    else {
      lPageSizeAttr = mPageSizeAttr;
    }

    //Evaluate page size attribute if it's an XPath
    ContextUElem lContextUElem = pRequestContext.getContextUElem();
    String lPageSizeResult;
    try {
      lPageSizeResult = lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), lPageSizeAttr);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to evaluate page size attribute", e);
    }

    //Parse XPath result to an int
    int lPageSizeInt;
    try {
      lPageSizeInt = Integer.parseInt(lPageSizeResult);
    }
    catch (NumberFormatException e) {
      throw new ExInternal("page-size attribute must be a valid integer for attribute value " + lPageSizeAttr, e);
    }

    return new EvaluatedPagerSetup(mDefinitionName, lPageSizeInt, mInvokeName, pMatchId);
  }
}
