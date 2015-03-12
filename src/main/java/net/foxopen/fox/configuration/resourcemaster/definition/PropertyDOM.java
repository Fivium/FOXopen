package net.foxopen.fox.configuration.resourcemaster.definition;

import net.foxopen.fox.dom.DOM;

public class PropertyDOM {
  private final DOM mDOM;
  private final String mLocation;

  public PropertyDOM(DOM pDOM, String pLocation) {
    mDOM = pDOM;
    mLocation = pLocation;
  }

  public DOM getDOM() {
    return mDOM;
  }

  public String getLocation() {
    return mLocation;
  }
}
