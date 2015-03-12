package net.foxopen.fox.enginestatus;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class StatusCollection
implements ContainerStatusItem {

  private final List<StatusItem> mItemList = new ArrayList<>();
  private final String mCollectionMnem;

  public StatusCollection(String pCollectionMnem) {
    mCollectionMnem = pCollectionMnem;
  }

  public static StatusCollection fromStringSet(String pCollectionMnem, Collection<String> pStrings) {
    StatusCollection lCollection = new StatusCollection(pCollectionMnem);
    for(String lString : pStrings) {
      lCollection.addItem(new StatusText(lString));
    }
    return lCollection;
  }

  public static StatusCollection fromStringMap(String pCollectionMnem, Map<String, String> pStringMap) {
    StatusCollection lCollection = new StatusCollection(pCollectionMnem);
    for(Map.Entry<String, String> lEntry : pStringMap.entrySet()) {
      lCollection.addItem(new StatusMessage(lEntry.getKey(), lEntry.getValue()));
    }
    return lCollection;
  }

  @Override
  public String getMnem() {
    return mCollectionMnem;
  }

  @Override
  public MessageLevel getMaxMessageSeverity() {
    return StatusCategory.getMaxChildMessageSeverity(mItemList);
  }

  public StatusCollection addItem(StatusItem pStatusItem) {
    mItemList.add(pStatusItem);
    return this;
  }

  @Override
  public StatusItem getNestedItem(String pItemMnem) {
    return StatusCategory.getNestedItem(mItemList, pItemMnem);
  }

  @Override
  public void serialiseHTML(Writer pWriter, StatusSerialisationContext pSerialisationContext)
  throws IOException {

    if (mItemList.size() > 0) {
      pWriter.append("<ul>");
      for (StatusItem lItem : mItemList) {
        pWriter.append("<li>");
        lItem.serialiseHTML(pWriter, pSerialisationContext);
        pWriter.append("</li>");
      }
      pWriter.append("</ul>");
    }
    else {
      pWriter.append("<span>Nothing to display.</span>");
    }
  }
}
