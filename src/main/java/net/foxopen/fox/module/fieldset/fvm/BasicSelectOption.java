package net.foxopen.fox.module.fieldset.fvm;


class BasicSelectOption
implements FieldSelectOption {

  private final String mDisplayKey;
  private final boolean mIsSelected;
  private final boolean mIsNullEntry;
  private final String mExternalValue;
  private final boolean mDisabled;

  BasicSelectOption(String pDisplayKey, boolean pIsSelected, boolean pIsNullEntry, String pExternalValue, boolean pDisabled) {
    mDisplayKey = pDisplayKey;
    mIsSelected = pIsSelected;
    mIsNullEntry = pIsNullEntry;
    mExternalValue = pExternalValue;
    mDisabled = pDisabled;
  }

  /**
   * Basic constructor for a FieldSelectOption which provides defauls for the null entry and disabled properties (both false).
   * @param pDisplayKey
   * @param pIsSelected
   * @param pExternalValue
   */
  BasicSelectOption(String pDisplayKey, boolean pIsSelected, String pExternalValue) {
    this(pDisplayKey, pIsSelected, false, pExternalValue, false);
  }

  @Override
  public String getDisplayKey() {
    return mDisplayKey;
  }

  @Override
  public String getExternalFieldValue() {
    return mExternalValue;
  }

  @Override
  public boolean isSelected() {
    return mIsSelected;
  }

  @Override
  public boolean isHistorical() {
    return false;
  }

  @Override
  public boolean isDisabled() {
    return mDisabled;
  }

  @Override
  public boolean isNullEntry() {
    return mIsNullEntry;
  }

  @Override
  public String getAdditionalProperty(String pPropertyName) {
    return null;
  }
}
