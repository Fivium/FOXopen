package net.foxopen.fox.module.fieldset.fvm;


import java.util.Collections;
import java.util.Map;

class BasicSelectOption
implements FieldSelectOption {

  private final String mDisplayKey;
  private final boolean mIsSelected;
  private final NullOptionType mNullOptionType;
  private final String mExternalValue;
  private final boolean mDisabled;
  private final Map<String, String> mAdditionalProperties;

  BasicSelectOption(String pDisplayKey, boolean pIsSelected, NullOptionType pNullOptionType, String pExternalValue, boolean pDisabled) {
    mDisplayKey = pDisplayKey;
    mIsSelected = pIsSelected;
    mNullOptionType = pNullOptionType;
    mExternalValue = pExternalValue;
    mDisabled = pDisabled;
    mAdditionalProperties = Collections.emptyMap();
  }

  BasicSelectOption(String pDisplayKey, boolean pIsSelected, NullOptionType pNullOptionType, String pExternalValue, boolean pDisabled, Map<String, String> pAdditionalProperties) {
    mDisplayKey = pDisplayKey;
    mIsSelected = pIsSelected;
    mNullOptionType = pNullOptionType;
    mExternalValue = pExternalValue;
    mDisabled = pDisabled;
    mAdditionalProperties = pAdditionalProperties;
  }

  /**
   * Basic constructor for a FieldSelectOption which provides defaults for the null entry and disabled properties (all false).
   * @param pDisplayKey
   * @param pIsSelected
   * @param pExternalValue
   */
  BasicSelectOption(String pDisplayKey, boolean pIsSelected, String pExternalValue) {
    this(pDisplayKey, pIsSelected, NullOptionType.NOT_NULL, pExternalValue, false);
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
    return mNullOptionType == NullOptionType.KEY_NULL;
  }

  @Override
  public boolean isMissingEntry() {
    return mNullOptionType == NullOptionType.KEY_MISSING;
  }

  @Override
  public String getAdditionalProperty(String pPropertyName) {
    return mAdditionalProperties.get(pPropertyName);
  }
}
