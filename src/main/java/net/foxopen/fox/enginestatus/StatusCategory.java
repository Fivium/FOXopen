package net.foxopen.fox.enginestatus;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.banghandler.BangHandler;
import net.foxopen.fox.ex.ExInternal;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class StatusCategory
implements ContainerStatusItem {

  private final String mCategoryMnem;
  private final String mTitle;
  private final boolean mDefaultExpanded;

  /** Messages which persist between refreshes */
  private Map<String, NamedStatusItem> mStickyMessages = new TreeMap<>();

  private Map<String, NamedStatusItem> mTransientMessages = new TreeMap<>();
  private Map<String, StatusTable> mTables = new TreeMap<>();
  private Map<String, StatusAction> mActions = new TreeMap<>();

  StatusCategory(String pCategoryMnem, String pTitle, boolean pDefaultExpanded) {
    mCategoryMnem = pCategoryMnem;
    mTitle = pTitle;
    mDefaultExpanded = pDefaultExpanded;
  }

  @Override
  public String getMnem() {
    return mCategoryMnem;
  }

  @Override
  public MessageLevel getMaxMessageSeverity() {
    try {
      Set<NamedStatusItem> lMessagesAndTables = new HashSet<>(mTransientMessages.values());
      lMessagesAndTables.addAll(mStickyMessages.values());
      lMessagesAndTables.addAll(mTables.values());
      return EngineStatus.getMaxChildMessageSeverity(lMessagesAndTables);
    }
    catch (Exception e) {
      //Don't allow errors caused by status objects to break the consumer
      return MessageLevel.ERROR;
    }
  }

  public String getTitle() {
    return mTitle;
  }

  @Override
  public StatusItem getNestedItem(String pItemMnem) {
    Set<NamedStatusItem> lMessagesAndTables = new HashSet<>(mTransientMessages.values());
    lMessagesAndTables.addAll(mStickyMessages.values());
    lMessagesAndTables.addAll(mTables.values());
    return EngineStatus.getNestedItem(lMessagesAndTables, pItemMnem);
  }

  /**
   * Sets a "sticky" message on this cateogry which will persist across refreshes of the category. This should only be
   * used to save a message which is impractical to generate in a StatusProvider, i.e. the result of a 1-time initialisation
   * method.
   * @param pMessageTitle
   * @param pMessageBody
   * @param pMessageLevel
   */
  public void setMessage(String pMessageTitle, String pMessageBody, MessageLevel pMessageLevel) {
    mStickyMessages.put(pMessageTitle, new StatusMessage(pMessageTitle, pMessageBody, pMessageLevel));
  }

  public boolean isExpanded() {
    return mDefaultExpanded;
  }

  /**
   * Resolves a detail message corresponding to the given path.
   * @param pDetailPath
   * @return
   */
  public StatusDetail resolveDetail(String pDetailPath) {

    StringBuilder lDetailPath = new StringBuilder(pDetailPath);

    ContainerStatusItem lCurrentStatusItem = this;
    while(lDetailPath.length() > 0) {
      String lCurrentMnem = XFUtil.pathPopHead(lDetailPath, true);
      StatusItem lNestedItem = lCurrentStatusItem.getNestedItem(lCurrentMnem);

      if(lNestedItem != null) {
        if(lDetailPath.length() == 0) {
          //Leaf should be a detail
          if(lNestedItem instanceof StatusDetail) {
            return (StatusDetail) lNestedItem;
          }
          else {
            throw new ExInternal("Leaf StatusItem must be a StatusDetail, was a " + lNestedItem.getClass().getSimpleName());
          }
        }
        else {
          if(lNestedItem instanceof ContainerStatusItem) {
            lCurrentStatusItem = (ContainerStatusItem) lNestedItem;
          }
          else {
            throw new ExInternal("StatusItem for mnem " + lCurrentMnem + " has no contents (is a " + lNestedItem.getClass().getSimpleName() + ")");
          }
        }
      }
      else {
        throw new ExInternal("StatusItem not found for mnem " + lCurrentMnem);
      }
    }

    throw new ExInternal("Failed to find StausItem for path " + pDetailPath);
  }

  @Override
  public void serialiseHTML(Writer pWriter, StatusSerialisationContext pSerialisationContext)
  throws IOException {

    pSerialisationContext.setCurrentCategory(mCategoryMnem);

    Map<String, NamedStatusItem> lMessages = new TreeMap<>(mTransientMessages);
    lMessages.putAll(mStickyMessages);

    if(lMessages.size() > 0) {
      pWriter.append("<table width=\"100%\" class=\"table table-striped\"><tbody>");
      for(NamedStatusItem lItem : lMessages.values()) {
        pWriter.append("<tr><td class=\"messageTitle\">");
        pWriter.append(lItem.getItemName());
        pWriter.append("</td><td>");
        if(lItem instanceof StatusDetail) {
          ((StatusDetail) lItem).serialiseHTML(pWriter, pSerialisationContext, "View");
        }
        else if(lItem instanceof StatusMessage) {
          ((StatusMessage) lItem).serialiseHTML(pWriter, pSerialisationContext, false);
        }
        else {
          lItem.serialiseHTML(pWriter, pSerialisationContext);
        }
        pWriter.append("</td></tr>");
      }
      pWriter.append("</table>");
    }

    if(mActions.size() > 0) {
      for (StatusAction lAction : mActions.values()) {
        lAction.serialiseHTML(pWriter, pSerialisationContext);
      }
    }

    for(StatusTable lTable : mTables.values()) {
      lTable.serialiseHTML(pWriter, pSerialisationContext);
    }
  }

  /**
   * Clears all transient StatusItems from this category and returns a destination object for new statuses to be written
   * to.
   * @return Fresh StatusDestination.
   */
  StatusDestination createStatusDestination() {

    mActions.clear();
    mTransientMessages.clear();
    mTables.clear();

    return new CategoryDestination();
  }

  /**
   * Destination implementation for transient messages.
   */
  private class CategoryDestination
  implements StatusDestination {

    /**
     * Adds a table to the status report. Consumers must subsequently call {@link StatusTable#setRowProvider} to give
     * the table a row source.
     * @param pTableName Title/summary of the table.
     * @param pColumnNames 1 or more column names. Calls to {@link StatusTable.Row#setColumn} will add columns in the order
     *                     of this array.
     * @return Created table.
     */
    @Override
    public StatusTable addTable(String pTableName, String... pColumnNames) {
      StatusTable lStatusTable = new StatusTable(pTableName, pColumnNames);
      mTables.put(pTableName, lStatusTable);
      return lStatusTable;
    }

    @Override
    public void addMessage(String pMessageTitle, String pMessageBody) {
      mTransientMessages.put(pMessageTitle, new StatusMessage(pMessageTitle, pMessageBody, MessageLevel.INFO));
    }

    @Override
    public void addMessage(String pMessageTitle, String pMessageBody, MessageLevel pLevel) {
      mTransientMessages.put(pMessageTitle, new StatusMessage(pMessageTitle, pMessageBody, pLevel));
    }

    @Override
    public void addDetailMessage(String pMessageTitle, StatusDetail.Provider pStatusDetailProvider) {
      mTransientMessages.put(pMessageTitle, new StatusDetail(pMessageTitle, pStatusDetailProvider));
    }

    @Override
    public void addAction(String pPrompt, BangHandler pBangHandler) {
      mActions.put(pPrompt, new StatusAction(pPrompt, pBangHandler, Collections.<String, String>emptyMap()));
    }

    @Override
    public void addAction(String pPrompt, String pAbsoluteURI) {
      mActions.put(pPrompt, new StatusAction(pPrompt, pAbsoluteURI));
    }

    @Override
    public void addAction(String pPrompt, BangHandler pBangHandler, Map<String, String> pParamMap) {
      mActions.put(pPrompt, new StatusAction(pPrompt, pBangHandler, pParamMap));
    }
  }
}
