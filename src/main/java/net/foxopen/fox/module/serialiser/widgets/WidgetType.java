package net.foxopen.fox.module.serialiser.widgets;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import net.foxopen.fox.module.datanode.EvaluatedNode;


/**
 * Tuple of builder type and plus-ness.
 */
public class WidgetType {

  private static final Map<WidgetBuilderType, WidgetType> mStandardWidgetMap = new EnumMap<>(WidgetBuilderType.class);
  private static final Map<WidgetBuilderType, WidgetType> mPlusWidgetMap = new EnumMap<>(WidgetBuilderType.class);

  private static final Set<WidgetBuilderType> ALLOWED_PLUS_WIDGETS = EnumSet.of(
    WidgetBuilderType.BUTTON,
    WidgetBuilderType.DATE,
    WidgetBuilderType.DATE_TIME,
    WidgetBuilderType.HTML,
    WidgetBuilderType.INPUT,
    WidgetBuilderType.LINK,
    WidgetBuilderType.SEARCH_SELECTOR,
    WidgetBuilderType.SELECTOR,
    WidgetBuilderType.TIMER
  );

  static {
    for(WidgetBuilderType lBuilderType : WidgetBuilderType.values()) {
      mStandardWidgetMap.put(lBuilderType, new WidgetType(lBuilderType, false));
    }

    //Set up plus widgets
    for(WidgetBuilderType lBuilderType : ALLOWED_PLUS_WIDGETS) {
      mPlusWidgetMap.put(lBuilderType, new WidgetType(lBuilderType, true));
    }
  }

  private final WidgetBuilderType mBuilderType;
  private final boolean mIsPlusWidget;

  /**
   * Non-plus widget.
   * @param pWidgetBuilderType
   * @return
   */
  public static WidgetType fromBuilderType(WidgetBuilderType pWidgetBuilderType) {
    return mStandardWidgetMap.get(pWidgetBuilderType);
  }

  /**
   * Can return null.
   * @param pExternalString
   * @return
   */
  public static WidgetType fromString(String pExternalString, EvaluatedNode pEvalNode) {

    WidgetBuilderType lBuilderType = WidgetBuilderType.fromString(pExternalString.replaceAll("\\+", ""), pEvalNode, true);
    boolean lIsPlusWidget = pExternalString.endsWith("+");

    if(lIsPlusWidget) {
      return mPlusWidgetMap.get(lBuilderType);
    }
    else {
      return mStandardWidgetMap.get(lBuilderType);
    }
  }

  private WidgetType(WidgetBuilderType pBuilderType, boolean pIsPlusWidget) {
    mBuilderType = pBuilderType;
    mIsPlusWidget = pIsPlusWidget;
  }

  public WidgetBuilderType getBuilderType() {
    return mBuilderType;
  }

  public boolean isIsPlusWidget() {
    return mIsPlusWidget;
  }
}
