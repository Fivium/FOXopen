package net.foxopen.fox.module;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackFlag;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DisplayOrder {
  private static Comparator<DisplayOrderSortable> mDisplayOrderComparator = new DisplayOrderComparator();
  public static final String AUTO_ATTR_VALUE = "auto";

  /**
   * Similar to a Collections.sort with the DisplayOrderComparator but with a secondary pass to order based on
   * displayBefore/After
   *
   * @param pSortableList List of items to sort, modified in place
   */
  public static void sort(List<? extends DisplayOrderSortable> pSortableList) {
    // Sort the list by regular displayOrder attributes
    Collections.sort(pSortableList, mDisplayOrderComparator);

    /*
      Create the initial map of items to their "sort" order, a padded number representing their position in the list
      after being sorted with the DisplayOrderComparator.

      The padding leaves a number either side of the items position to possibly later be taken up by other items that
      display before/after it.

      The list will be transformed from:
      pSortableList[0] = ITEM_1
      pSortableList[1] = ITEM_2
      pSortableList[2] = ITEM_3
      pSortableList[3] = ITEM_4

      To the map lSortOrderMap:
      ITEM_1 => 2
      ITEM_2 => 5
      ITEM_3 => 8
      ITEM_4 => 11
     */
    int lListSize = pSortableList.size();
    Map<String, Integer> lSortOrderMap = new HashMap<>(lListSize);
    boolean lDisplayBeforeAfterExistence = false;
    Integer lItemIndex = 0;
    for (DisplayOrderSortable lItem : pSortableList) {
      // "sorted" order starts off spaced out to fit potential items that display before/after them
      lSortOrderMap.put(lItem.getName(), (++lItemIndex * 3) - 1);

      // Check for the existence of displayBefore/After so we can skip other steps if they aren't needed
      if (!lDisplayBeforeAfterExistence && (!XFUtil.isNull(lItem.getDisplayBeforeAttribute()) || !XFUtil.isNull(lItem.getDisplayAfterAttribute()))) {
        lDisplayBeforeAfterExistence = true;
      }
    }

    if (lDisplayBeforeAfterExistence) {
      /*
        If the previous loop found items with displayBefore or displayAfter attributes then we need to shuffle those
        items around via their "sort" order number in the lSortOrderMap, using the numerical gaps created earlier.

        If ITEM_3 has displayBefore=ITEM_2 and ITEM_4 has displayAfter=ITEM_1 then the map lSortOrderMap will get transformed from:
        ITEM_1 => 2
        ITEM_2 => 5
        ITEM_3 => 8
        ITEM_4 => 11

        To:
        ITEM_1 => 2
        ITEM_2 => 5
        ITEM_3 => 4
        ITEM_4 => 3
       */
      for (DisplayOrderSortable lItem : pSortableList) {
        String lDisplayBeforeNode = lItem.getDisplayBeforeAttribute();
        String lDisplayAfterNode = lItem.getDisplayAfterAttribute();
        if (!XFUtil.isNull(lDisplayBeforeNode) && !XFUtil.isNull(lDisplayAfterNode)) {
          Track.alert("DisplayBeforeAfter", "displayBefore and displayAfter both defined on element '" + lItem.getName() + "', cannot handle this so will have no effect", TrackFlag.BAD_MARKUP);
        }
        else if (!XFUtil.isNull(lDisplayBeforeNode)) {
          Integer lTargetSortOrder = lSortOrderMap.get(lDisplayBeforeNode);
          if (lTargetSortOrder == null) {
            Track.info("DisplayBefore", "displayBefore defined on element '" + lItem.getName() + "' points to an unknown element name: " + lDisplayBeforeNode);
          }
          else {
            lSortOrderMap.put(lItem.getName(), lTargetSortOrder - 1);
          }
        }
        else if (!XFUtil.isNull(lDisplayAfterNode)) {
          Integer lTargetSortOrder = lSortOrderMap.get(lDisplayAfterNode);
          if (lTargetSortOrder == null) {
            Track.info("DisplayAfter", "displayAfter defined on element '" + lItem.getName() + "' points to an unknown element name: " + lDisplayAfterNode);
          }
          else {
            lSortOrderMap.put(lItem.getName(), lTargetSortOrder + 1);
          }
        }
      }

      /*
        Once the displayBefore and displayAfter items have had their "sort" order modified we can sort the list once more
        using the a new SortOrderComparator which takes in the SortOrderMap to use when comparing items.
       */
      Collections.sort(pSortableList, new SortOrderComparator(lSortOrderMap));
    }
  }

  /**
   * Comparator that compares the displayOrder attribute values of two DisplayOrderSortable items
   */
  private static class DisplayOrderComparator
    implements Comparator<DisplayOrderSortable> {

    public int compare(DisplayOrderSortable pSortableA, DisplayOrderSortable pSortableB) {
      // Compare displayOrder attributes on two EvaluatedNodeInfos
      String lDisplayOrderA = pSortableA.getDisplayOrder();
      String lDisplayOrderB = pSortableB.getDisplayOrder();

      // If the DisplayOrder == auto then it goes to the end of the list with numbered ones first
      if (DisplayOrder.AUTO_ATTR_VALUE.equals(lDisplayOrderA) && DisplayOrder.AUTO_ATTR_VALUE.equals(lDisplayOrderB)) {
        return 0;
      }
      else if (DisplayOrder.AUTO_ATTR_VALUE.equals(lDisplayOrderA)) {
        return 1;
      }
      else if (DisplayOrder.AUTO_ATTR_VALUE.equals(lDisplayOrderB)) {
        return -1;
      }
      else {
        try {
          return Integer.parseInt(lDisplayOrderA) - Integer.parseInt(lDisplayOrderB);
        }
        catch(NumberFormatException e) {
          throw new ExInternal("Invalid display order attribute - must be a valid integer. Got '" + lDisplayOrderA + "' from '" + pSortableA.getName() + "' and '" + lDisplayOrderB + "' from '" + pSortableB.getName() + "'", e);
        }
      }
    }
  }

  /**
   * Comparator that compares the "sort" order of two items and falls back to the DisplayOrderComparator if the "sort"
   * orders are equal
   */
  private static class SortOrderComparator
    implements Comparator<DisplayOrderSortable> {
    private final Map<String, Integer> mItemSortOrder;

    public SortOrderComparator(Map<String, Integer> pItemSortOrder) {
      super();
      mItemSortOrder = pItemSortOrder;
    }

    public int compare(DisplayOrderSortable pSortableA, DisplayOrderSortable pSortableB) {
      int lSortOrderA = mItemSortOrder.get(pSortableA.getName());
      int lSortOrderB = mItemSortOrder.get(pSortableB.getName());
      if (lSortOrderA != lSortOrderB) {
        // Compare "sort" order if they aren't the same
        return lSortOrderA - lSortOrderB;
      }
      else {
        // If they have the same "sort" order, order them relative to each other using regular displayOrder
        return mDisplayOrderComparator.compare(pSortableA, pSortableB);
      }
    }
  }
}
