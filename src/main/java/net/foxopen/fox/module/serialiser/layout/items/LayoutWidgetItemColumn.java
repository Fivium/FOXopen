package net.foxopen.fox.module.serialiser.layout.items;

import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;

public class LayoutWidgetItemColumn implements LayoutItemColumn {
  private final int mColSpan;
  private final EvaluatedNode mItemNodeInfo;
  private final boolean mIsPrompt;
  private final boolean mIsFiller;
  private final WidgetBuilder mWidgetBuilder;

  /**
   * Constructor for regular widgets
   *
   * @param pColSpan
   * @param pItemNodeInfo
   * @param pIsPrompt
   * @param pWidgetBuilder
   */
  public LayoutWidgetItemColumn(int pColSpan, EvaluatedNode pItemNodeInfo, boolean pIsPrompt, WidgetBuilder pWidgetBuilder) {
    mColSpan = pColSpan;
    mItemNodeInfo = pItemNodeInfo;
    mWidgetBuilder = pWidgetBuilder;
    mIsPrompt = pIsPrompt;
    mIsFiller = false;
  }

  /**
   * Constructor for filler columns
   *
   * @param pColSpan
   */
  public LayoutWidgetItemColumn(int pColSpan) {
    mColSpan = pColSpan;
    mIsFiller = true;
    mWidgetBuilder = null;
    mItemNodeInfo = null;
    mIsPrompt = false;
  }

  @Override
  public LayoutItemEnum getItemType() {
    return LayoutItemEnum.COLUMN;
  }

  @Override
  public int getColSpan() {
    return mColSpan;
  }

  @Override
  public boolean isFiller() {
    return mIsFiller;
  }

  public boolean isPrompt() {
    return mIsPrompt;
  }

  public EvaluatedNode getItemNode() {
    return mItemNodeInfo;
  }

  public WidgetBuilder getWidgetBuilder() {
    return mWidgetBuilder;
  }
}
