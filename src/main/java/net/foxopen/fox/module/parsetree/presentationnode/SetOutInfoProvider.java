package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.dom.paging.PagerSetup;
import net.foxopen.fox.module.parsetree.PageControlsPosition;

public interface SetOutInfoProvider {
  public String getMatch();

  public String getDatabasePaginationInvokeName();

  /**
   * Can return null.
   * @return
   */
  public PageControlsPosition getPageControlsPosition();

  public PagerSetup getDOMPagerSetup();
}
