package net.foxopen.fox.module.fieldset.fvm;

import net.foxopen.fox.module.mapset.MapSetEntry;

/**
 * FieldSelectOption implementation for a MapSetEntry.
 */
class MapSetSelectOption
implements FieldSelectOption {

  private final MapSetEntry mMapSetEntry;
  private final boolean mSelected;
  private final String mExternalValue;

  MapSetSelectOption(MapSetEntry pMapSetEntry, boolean pSelected, String pExternalValue) {
    mMapSetEntry = pMapSetEntry;
    mSelected = pSelected;
    mExternalValue = pExternalValue;
  }

  @Override
  public String getDisplayKey() {
    return mMapSetEntry.getKey();
  }

  @Override
  public String getExternalFieldValue() {
    return mExternalValue;
  }

  @Override
  public boolean isSelected() {
    return mSelected;
  }

  @Override
  public boolean isHistorical() {
    return mMapSetEntry.isHistorical();
  }

  @Override
  public boolean isDisabled() {
    return mMapSetEntry.isDisabled();
  }

  @Override
  public boolean isNullEntry() {
    return false;
  }

  @Override
  public boolean isMissingEntry() {
    return false;
  }

  @Override
  public String getAdditionalProperty(String pPropertyName) {
    return mMapSetEntry.getAdditionalPropertyString(pPropertyName);
  }
}
