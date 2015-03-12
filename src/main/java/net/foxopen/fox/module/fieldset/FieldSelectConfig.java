package net.foxopen.fox.module.fieldset;

//TODO PN - maybe SelectableWidgetType or something - subtype widget type with these options built in
//OR WidgetTypeConfig - extended by RadioWidgetConfig, SelectorWidgetConfig, etc...

public class FieldSelectConfig {
  
  private final boolean mAddKeyNull;
  private final boolean mAddKeyMissing;
  private final boolean mStrictBoolean;

  public FieldSelectConfig(boolean pAddKeyNull, boolean pAddKeyMissing, boolean pStrictBoolean) {
    mAddKeyNull = pAddKeyNull;
    mAddKeyMissing = pAddKeyMissing;
    mStrictBoolean = pStrictBoolean;
  }

  public boolean isAddKeyNull() {
    return mAddKeyNull;
  }

  public boolean isAddKeyMissing() {
    return mAddKeyMissing;
  }

  public boolean isStrictBoolean() {
    return mStrictBoolean;
  }
}
