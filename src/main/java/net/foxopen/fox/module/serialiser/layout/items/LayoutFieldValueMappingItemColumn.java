package net.foxopen.fox.module.serialiser.layout.items;

import net.foxopen.fox.module.fieldset.fvm.FieldSelectOption;

public class LayoutFieldValueMappingItemColumn implements LayoutItemColumn {
  private final int mColSpan;
  private final FieldSelectOption mFieldSelectOption;
  private final boolean mIsFiller;

  /**
   * Constructor for regular FVM items
   *
   * @param pColSpan
   */
  public LayoutFieldValueMappingItemColumn(int pColSpan, FieldSelectOption pFieldSelectOption) {
    mColSpan = pColSpan;
    mFieldSelectOption = pFieldSelectOption;
    mIsFiller = false;
  }

  /**
   * Constructor for filler items
   *
   * @param pColSpan
   */
  public LayoutFieldValueMappingItemColumn(int pColSpan) {
    mColSpan = pColSpan;
    mFieldSelectOption = null;
    mIsFiller = true;
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

  public FieldSelectOption getFieldSelectOption() {
    return mFieldSelectOption;
  }
}
