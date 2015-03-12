package net.foxopen.fox.dom.paging;


/**
 * PagerSetup with page size evaluated. This should be used to create new Pagers.
 */
public class EvaluatedPagerSetup {

  private final String mPaginationDefinition;
  private final int mPageSize;
  private final String mInvokeName;
  private final String mMatchId;

  EvaluatedPagerSetup(String pPaginationDefinition, int pPageSize, String pInvokeName, String pMatchId) {
    mPaginationDefinition = pPaginationDefinition;
    mPageSize = pPageSize;
    mInvokeName = pInvokeName;
    mMatchId = pMatchId;
  }

  /**
   * Can be null if no pagination definition was used to create this object.
   * @return
   */
  public String getPaginationDefinitionName() {
    return mPaginationDefinition;
  }

  public int getPageSize() {
    return mPageSize;
  }

  public String getInvokeName() {
    return mInvokeName;
  }

  /**
   * Can be null if no match ID was specified when this object was created.
   * @return
   */
  public String getMatchId() {
    return mMatchId;
  }
}
