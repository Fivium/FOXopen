package net.foxopen.fox.dom.paging;

import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.command.XDoIsolatedRunner;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.paging.style.PageControlStyle;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.facet.ModuleFacet;
import net.foxopen.fox.module.facet.ModuleFacetType;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.persistence.PersistableType;
import net.foxopen.fox.thread.persistence.PersistenceContext;
import net.foxopen.fox.thread.persistence.PersistenceMethod;
import net.foxopen.fox.thread.persistence.PersistenceResult;
import net.foxopen.fox.track.Track;

import java.util.Collection;
import java.util.Collections;


/**
 * A pager is a serialisable, stateful object which maintains information about a paginated query or DOM section
 * (namely the current page size, page number and row count). A pager is typically identified using an invoke name and
 * optionally a match ID in the case that a single invoke name could result in multiple pagers being created. <br/><br/>
 *
 * Pagers are resolved and created using a {@link PagerProvider}. Implementors of this abstract class should provide
 * behaviour specialisations based on the pager's requirements. <br/><br/>
 *
 * Note: Pagers are currently serialised automatically using Kryo. They must NOT contain any member variable references
 * to other 'live' objects.
 */
public abstract class Pager
implements ModuleFacet {

  private final String mPagerKey;

  private final String mModuleCallId;
  private final String mInvokeName;
  protected final String mMatchFoxId; //Can be null

  private final String mDefinitionName;

  private int mPageSize;
  private int mRowCount = 0;
  private int mCurrentPage = 1;

  protected Pager(String pPagerKey, String pModuleCallId, EvaluatedPagerSetup pEvalPagerSetup) {
    mPagerKey = pPagerKey;
    mModuleCallId = pModuleCallId;
    mMatchFoxId = pEvalPagerSetup.getMatchId();

    mInvokeName = pEvalPagerSetup.getInvokeName();

    mDefinitionName = pEvalPagerSetup.getPaginationDefinitionName();
    mPageSize = pEvalPagerSetup.getPageSize();
  }

  /**
   * Resolves the given path to a FOX ID. If the XPath is null, or the node does not exist or does not have a match ID,
   * empty string is returned. If the XPath returns too many nodes an error is raised.
   * @param pContextUElem For XPath evaluation.
   * @param pMatchXPath Can be null.
   * @return Match node FOX ID or empty string.
   */
  public static String getMatchIdOrNull(ContextUElem pContextUElem, String pMatchXPath, DOM pRelativeDOM) {
    if(!XFUtil.isNull(pMatchXPath)) {
      try {
        DOM lMatchDOM = pContextUElem.extendedXPath1E(pRelativeDOM, pMatchXPath);
        return lMatchDOM.getFoxId();
      }
      catch (ExTooFew e) {
        Track.info("PagerMatchAttribute", "Match XPath " + pMatchXPath + " found no nodes");
      }
      catch (ExTooMany | ExActionFailed e) {
        throw new ExInternal("Failed to evaluate match attribute for pager control", e);
      }
    }

    return "";
  }

  /**
   * Tests that the given page number is within the boundaries for this pager.
   * @param pPageNum Page number to test.
   * @return True if the page number is valid, false otherwise.
   */
  public boolean validatePageNumber(int pPageNum) {
    return validatePageNumber(pPageNum, getPageSize());
  }

  /**
   * Tests that the given page number will be within the boundaries for this pager given the specified page size. Use
   * this method for validating that a combined page size and page number change will be valid.
   * @param pPageNum Page number to test.
   * @param pForPageSize Hypothetical page size for this Pager.
   * @return
   */
  public boolean validatePageNumber(int pPageNum, int pForPageSize) {
    return !(pPageNum < 1 || pPageNum > getPageCount(pForPageSize));
  }

  /**
   * Changes this pager's page size and optionally goes to the specified new page if pOptionalNewPageNum is not null. If the
   * new page argument is null, the pager remains on the same page unless its current page exceeeds the page boundaries
   * after the page size change, in which case the current page is knocked down to the highest available page. Even if
   * the page number does not change, the goToPage logic is invoked to force a requery (for example) of the correct
   * number of rows.
   * @param pRequestContext For action running and persistence.
   * @param pNewPageSize New page size for this Pager, must be greater than 0.
   * @param pOptionalNewPageNum Page to go to after changing the page size, or null to perform the default behaviour.
   */
  public void changePageSize(ActionRequestContext pRequestContext, int pNewPageSize, Integer pOptionalNewPageNum) {
    if(pNewPageSize <= 0) {
      throw new ExInternal("Page size must be greater than 0");
    }

    mPageSize = pNewPageSize;

    Integer lNewPageNum = pOptionalNewPageNum;
    if(lNewPageNum == null) {
      //Knock back the page to the maximum available page if we're now exceeding the page count and user hasn't asked for a specific page
      lNewPageNum = getClosestActualPageNum();
    }

    //Always do a go to page to force a requery etc (also marks this pager for update)
    goToPage(pRequestContext, lNewPageNum);
  }

  /**
   * Establishes the closest available page number to the current page. This will either be the current page or a lower
   * value. This should be used to switch to an actual page after changing the pager's row count.
   * @return
   */
  protected int getClosestActualPageNum() {
    if(mCurrentPage > getPageCount()) {
      return getPageCount();
    }
    else {
      //The page we're on is still valid and user hasn't asked for another one
      return mCurrentPage;
    }
  }

  /**
   * Changes this Pager's current page, performing any requerying as appropriate, and running any pre/post change actions
   * if defined. If the specified page is out of this Pager's range, an exception is thrown.
   * @param pRequestContext For action running and persistence.
   * @param pPageNum Page to go to.
   */
  public void goToPage(ActionRequestContext pRequestContext, int pPageNum) {

    if(!validatePageNumber(pPageNum)) {
      throw new ExInternal(pPageNum + " is not a valid page number for this pager");
    }

    //Resolve the match node if we have a match id
    DOM lMatchDOM = null;
    if(!XFUtil.isNull(mMatchFoxId)) {
      lMatchDOM = pRequestContext.getContextUElem().getElemByRef(mMatchFoxId);
    }

    //Run pre-page
    XDoControlFlow lPrePageActionResult = runPrePageAction(pRequestContext, lMatchDOM);
    if(lPrePageActionResult.canContinue()) {
      setCurrentPage(pPageNum);

      //Invoke the logic to change the page contents (if any)
      goToPageInternal(pRequestContext, pPageNum, lMatchDOM);

      //Run post-page
      runPostPageAction(pRequestContext, lMatchDOM);
    }
    else {
      Track.info("GoToPageInterrupted", "ACTIONBREAK prevented go to page from completing");
    }

    pRequestContext.getPersistenceContext().requiresPersisting(this, PersistenceMethod.UPDATE);
  }

  protected abstract void goToPageInternal(ActionRequestContext pRequestContext, int pPageNum, DOM pMatchDOM);

  public final String getPagerKey() {
    return mPagerKey;
  }

  public String getModuleCallId() {
    return mModuleCallId;
  }

  protected String getInvokeName() {
    return mInvokeName;
  }

  public int getPageSize() {
    return mPageSize;
  }

  public int getRowCount() {
    return mRowCount;
  }

  public void setRowCount(int pRowCount) {
    mRowCount = pRowCount;
  }

  public int getCurrentPage() {
    return mCurrentPage;
  }

  protected void setCurrentPage(int pCurrentPage) {
    mCurrentPage = pCurrentPage;
  }

  protected int getCurrentPageStartRowNum() {
    return ((mCurrentPage-1) * mPageSize) + 1;
  }

  protected int getCurrentPageEndRowNum() {
    return mCurrentPage * mPageSize;
  }

  public int getPageCount() {
    return getPageCount(getPageSize());
  }

  private int getPageCount(int pForPageSize) {
    return (int) Math.ceil(getRowCount() / (double) pForPageSize);
  }


  @Override
  public Collection<PersistenceResult> create(PersistenceContext pPersistenceContext) {
    pPersistenceContext.getSerialiser().createModuleFacet(this);
    return Collections.singleton(new PersistenceResult(this, PersistenceMethod.CREATE));
  }

  @Override
  public Collection<PersistenceResult> update(PersistenceContext pPersistenceContext) {
    pPersistenceContext.getSerialiser().updateModuleFacet(this);
    return Collections.singleton(new PersistenceResult(this, PersistenceMethod.UPDATE));
  }

  @Override
  public Collection<PersistenceResult> delete(PersistenceContext pPersistenceContext) {
    return Collections.emptySet();
  }

  @Override
  public PersistableType getPersistableType() {
    return PersistableType.MODULE_FACET;
  }

  /**
   *
   * @param pRequestContext
   * @param pMatchDOM May be null.
   * @return
   */
  public XDoControlFlow runPrePageAction(ActionRequestContext pRequestContext, DOM pMatchDOM) {

    ContextUElem lLocalContext = pRequestContext.getContextUElem().localise("pre-page-xdo");
    try {
      if(pMatchDOM != null) {
        lLocalContext.setUElem(ContextLabel.ATTACH, pMatchDOM);
      }
      XDoCommandList lPrePageXDo = null;
      if(!XFUtil.isNull(mDefinitionName)) {
        lPrePageXDo = pRequestContext.getCurrentModule().getPagerDefinitionByName(mDefinitionName).getPrePageAction();
      }

      if (lPrePageXDo != null) {
        XDoIsolatedRunner lRunner = pRequestContext.createIsolatedCommandRunner(true);
        return lRunner.runCommandsAndComplete(pRequestContext, lPrePageXDo);
      }
      else {
        prePageDefaultAction(pMatchDOM);
        return XDoControlFlowContinue.instance();
      }
    }
    catch (Throwable ex) {
      throw new ExInternal ("Failed to run pre-page action for definition '"+mDefinitionName+"'.", ex);
    }
    finally {
      lLocalContext.delocalise("pre-page-xdo");
    }
  }

  /**
   *
   * @param pMatchDOM May be null.
   */
  protected abstract void prePageDefaultAction(DOM pMatchDOM);

  /**
   *
   * @param pRequestContext
   * @param pMatchDOM May be null.
   */
  public void runPostPageAction(ActionRequestContext pRequestContext, DOM pMatchDOM) {

    ContextUElem lLocalContext = pRequestContext.getContextUElem().localise("post-page-xdo");
    try {
      if(pMatchDOM != null) {
        lLocalContext.setUElem(ContextLabel.ATTACH, pMatchDOM);
      }
      XDoCommandList lPostPageXDo = null;

      if(!XFUtil.isNull(mDefinitionName)) {
        lPostPageXDo = pRequestContext.getCurrentModule().getPagerDefinitionByName(mDefinitionName).getPostPageAction();
      }

      if (lPostPageXDo != null) {
        XDoIsolatedRunner lRunner = pRequestContext.createIsolatedCommandRunner(true);
        lRunner.runCommandsAndComplete(pRequestContext, lPostPageXDo);
      }
      else {
        postPageDefaultAction(pMatchDOM);
      }
    }
    catch (Throwable ex) {
      throw new ExInternal ("Failed to run post-page action for definition '"+mDefinitionName+"'.", ex);
    }
    finally {
      lLocalContext.delocalise("post-page-xdo");
    }
  }

  /**
   *
   * @param pMatchDOM May be null.
   */
  protected abstract void postPageDefaultAction(DOM pMatchDOM);

  public void resetPager(ActionRequestContext pRequestContext, DOM pMatchDOM) {
    setRowCount(0);
    setCurrentPage(1);

    resetPagerInternal(pRequestContext, pMatchDOM);
  }

  /**
   * Implementors should perform any specialised reset actions in this method.
   * @param pRequestContext
   * @param pMatchDOM
   */
  protected abstract void resetPagerInternal(ActionRequestContext pRequestContext, DOM pMatchDOM);

  protected PageControlStyle getPageControlStyle(Mod pModule) {
    PageControlStyle lPageControlStyle = null;
    if(!XFUtil.isNull(mDefinitionName)) {
      lPageControlStyle = pModule.getPagerDefinitionByName(mDefinitionName).getPageControlStyle();
    }

    if(lPageControlStyle != null) {
      return lPageControlStyle;
    }
    else {
      return PageControlStyle.getDefault(this);
    }
  }

  public PageControlStyle getPageControlStyle(ActionRequestContext pRequestContext) {
    return getPageControlStyle(pRequestContext.getCurrentModule());
  }

  @Override
  public String getFacetKey() {
    return getPagerKey();
  }

  @Override
  public ModuleFacetType getFacetType() {
    return ModuleFacetType.PAGER;
  }
}
