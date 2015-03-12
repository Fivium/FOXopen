package net.foxopen.fox.enginestatus;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.banghandler.BangHandler;
import net.foxopen.fox.ex.ExInternal;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class StatusCategory
implements ContainerStatusItem {

  private final String mCategoryMnem;
  private final String mTitle;
  private final int mDisplayOrder;
  private final boolean mDefaultExpanded;

  private Map<String, NamedStatusItem> mMessages = new TreeMap<>();
  private Map<String, StatusTable> mTables = new TreeMap<>();
  private Map<String, StatusAction> mActions = new TreeMap<>();

  StatusCategory(String pCategoryMnem, String pTitle, int pDisplayOrder, boolean pDefaultExpanded) {
    mCategoryMnem = pCategoryMnem;
    mTitle = pTitle;
    mDisplayOrder = pDisplayOrder;
    mDefaultExpanded = pDefaultExpanded;
  }

  @Override
  public String getMnem() {
    return mCategoryMnem;
  }

  @Override
  public MessageLevel getMaxMessageSeverity() {
    try {
      Set<NamedStatusItem> lMessagesAndTables = new HashSet<>(mMessages.values());
      lMessagesAndTables.addAll(mTables.values());
      return getMaxChildMessageSeverity(lMessagesAndTables);
    }
    catch (Exception e) {
      //Don't allow errors caused by status objects to break the consumer
      return MessageLevel.ERROR;
    }
  }

  public String getTitle() {
    return mTitle;
  }

  static StatusItem getNestedItem(Collection<? extends StatusItem> pChildren, String pItemMnem) {
    for(StatusItem lItem : pChildren) {
      if(pItemMnem.equals(lItem.getMnem())) {
        return lItem;
      }
    }
    return null;
  }

  //TODO PN move somewhere more sensible
  static MessageLevel getMaxChildMessageSeverity(Collection<? extends StatusItem> pChildren) {
    MessageLevel lMax = MessageLevel.INFO;
    for(StatusItem lItem : pChildren) {
      if(lItem.getMaxMessageSeverity().intValue() > lMax.intValue()) {
        lMax = lItem.getMaxMessageSeverity();
      }
    }
    return lMax;
  }

  @Override
  public StatusItem getNestedItem(String pItemMnem) {
    Set<NamedStatusItem> lMessagesAndTables = new HashSet<>(mMessages.values());
    lMessagesAndTables.addAll(mTables.values());
    return getNestedItem(lMessagesAndTables, pItemMnem);
  }

  public int getDisplayOrder() {
    return mDisplayOrder;
  }

  public boolean isExpanded() {
    return mDefaultExpanded;
  }

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

  public StatusTable addTable(String pTableName, String... pColumnNames) {
    StatusTable lStatusTable = new StatusTable(pTableName, pColumnNames);
    mTables.put(pTableName, lStatusTable);
    return lStatusTable;
  }

  public void addMessage(String pMessageTitle, String pMessageBody) {
    mMessages.put(pMessageTitle, new StatusMessage(pMessageTitle, pMessageBody, MessageLevel.INFO));
  }

  /**
   * Create a message in this category or update an existing message if one with the same title already exists.
   *
   * @param pMessageTitle Message title, also used as the key for messages in categories
   * @param pMessageBody Message text content
   * @param pLevel
   * @return Reference to the updated Message
   */
  public void addMessage(String pMessageTitle, String pMessageBody, MessageLevel pLevel) {
    mMessages.put(pMessageTitle, new StatusMessage(pMessageTitle, pMessageBody, pLevel));
  }

  public void addDetailMessage(String pMessageTitle, StatusDetail.Provider pStatusDetailProvider) {
    mMessages.put(pMessageTitle, new StatusDetail(pMessageTitle, pStatusDetailProvider));
  }

  public void addAction(String pPrompt, BangHandler pBangHandler) {
    mActions.put(pPrompt, new StatusAction(pPrompt, pBangHandler, Collections.<String, String>emptyMap()));
  }

  public void addAction(String pPrompt, BangHandler pBangHandler, Map<String, String> pParamMap) {
    mActions.put(pPrompt, new StatusAction(pPrompt, pBangHandler, pParamMap));
  }

//  public StatusCollection addCollection(String pPrompt) {
//    StatusCollection lCollection = new StatusCollection();
//    mStatusItems.put(pPrompt, lCollection);
//    return lCollection;
//  }

  @Override
  public void serialiseHTML(Writer pWriter, StatusSerialisationContext pSerialisationContext)
  throws IOException {

    pSerialisationContext.setCurrentCategory(mCategoryMnem);

    if(mMessages.size() > 0) {
      pWriter.append("<table width=\"100%\" class=\"table table-striped\"><tbody>");
      for(NamedStatusItem lItem : mMessages.values()) {
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
}
