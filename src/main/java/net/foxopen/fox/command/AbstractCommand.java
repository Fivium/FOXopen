package net.foxopen.fox.command;

import com.google.common.base.Joiner;
import net.foxopen.fox.StringUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.Trackable;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared implementation details between built in commands and plug in commands (namely attribute parsing and presentation).
 */
public abstract class AbstractCommand
implements Command, Trackable {

  /** The name of the command. */
  private String mCommandName;

  /** A map of command attributes, name->value. */
  protected Map<String, String> mAttributeMap;

  protected AbstractCommand(DOM pCommandDOM) {
    mCommandName = pCommandDOM.getLocalName();
    mAttributeMap = new HashMap<>();
    mAttributeMap.putAll(pCommandDOM.getAttributeMap());
  }

  public String toString() {
    return getClass().getName()+": "+StringUtil.mapToDelimitedString(this.mAttributeMap," ");
  }


  /**
  * Returns the value of the specified command attribute.
  *
  * @param name the name of the attribute
  * @return the value of the specified atribute, or null if
  *         the attribute does not exist.
  */
  protected String getAttribute(String name) {
    return mAttributeMap.get(name);
  }

  /**
  * Returns the value of the specified command attribute, or
  * the specified default value for the attribute if the
  * attribute dos not exist.
  *
  * @param name the name of the attribute
  * @param defaultValue a default value to return if the named attribute does not exist.
  * @return the value of the specified atribute, or the default value specified if
  *         the attribute does not exist.
  */
  protected String getAttribute(String name, String defaultValue) {
    String value = mAttributeMap.get(name);
    return (value != null ? value : defaultValue);
  }

  /**
  * Returns Either 1) The trimmed value of the specified command attribute, or
  *                2) Default value for the attribute if the attribute is blank or does not exist
  *
  * @param name the name of the attribute
  * @param defaultValue a default value to return if the named attribute does not exist ir its blank
  * @return the value of the specified atribute, or the default value specified if
  *         the attribute does not exist.
  */
  protected String getNonBlankAttribute(String name, String defaultValue) {
    String value = mAttributeMap.get(name);
    if (value != null) {
      value.trim();
    }
    if (value == null || value.equals("") ) {
      value = defaultValue;
    }
    return value;
  }

  /**
  * Determines if the specified attribute has been supplied.
  *
  * @param name the name of the attribute
  * @return true if the attribute was supplied, false otherwise.
  */
  protected boolean isAttributeSupplied(String name) {
    return mAttributeMap.get(name) != null;
  }

  @Override
  public void writeTrackData() {
    for(Map.Entry<String, String> lAttr : mAttributeMap.entrySet()) {
      Track.addAttribute(lAttr.getKey(), lAttr.getValue());
    }
  }

  @Override
  public String getDebugInfo() {
    return getCommandName() + " " + Joiner.on(", ").withKeyValueSeparator("=").join(mAttributeMap);
  }

  public final String getName() {
    return mCommandName;
  }

  @Override
  public final String getCommandName() {
    return mCommandName;
  }
}
