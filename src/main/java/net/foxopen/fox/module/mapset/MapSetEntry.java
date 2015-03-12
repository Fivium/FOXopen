package net.foxopen.fox.module.mapset;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExInternal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Display information about an entry in a mapset, including its key and disabled/historical status. This object also
 * contains additional "properties" which developers may have generated to supplement a mapset definition or provide
 * additional functionality for consuming widgets. These properties are retrievable by consumers and are written to the
 * mapset "rec" element if this entry is serialised.
 */
public class MapSetEntry {

  public static final String HISTORICAL = "historical";
  public static final String DISABLED = "disabled";

  private final String mKey;
  private final boolean mHistorical;
  private final boolean mDisabled;

  private static final Set<String> SPECIAL_MAPSET_ELEMENT_NAMES = new HashSet<>(Arrays.asList(MapSet.KEY_ELEMENT_NAME, MapSet.DATA_ELEMENT_NAME, HISTORICAL, DISABLED));

  private final Set<DOM> mAdditionalProperties;

  static MapSetEntry createFromDOM(DOM pRecordDOM) {

    String lKeyString;

    try {
      lKeyString = pRecordDOM.get1S("key");
    }
    catch (ExCardinality e) {
      throw new ExInternal("Invalid mapset DOM - missing 'key' element", e);
    }

    if(XFUtil.isNull(lKeyString)) {
      throw new ExInternal("Invalid mapset DOM - 'key' element cannot be null");
    }

    Boolean lHistorical = Boolean.valueOf(XFUtil.nvl(pRecordDOM.getAttrOrNull(HISTORICAL), pRecordDOM.get1SNoEx(HISTORICAL)));
    Boolean lDisabled = Boolean.valueOf(pRecordDOM.get1SNoEx(DISABLED));

    Set<DOM> lAdditionalProperties = new HashSet<>();

    for(DOM lChildElem : pRecordDOM.getChildElements()) {
      String lElemName = lChildElem.getName();
      if(!SPECIAL_MAPSET_ELEMENT_NAMES.contains(lElemName)) {
        lAdditionalProperties.add((lChildElem.clone(true)));
      }
    }

    return new MapSetEntry(lKeyString, lHistorical, lDisabled, lAdditionalProperties.size() > 0 ? lAdditionalProperties : Collections.<DOM>emptySet());
  }

  private MapSetEntry(String pKey, boolean pHistorical, boolean pDisabled, Set<DOM> pAdditionalProperties) {
    mKey = pKey;
    mHistorical = pHistorical;
    mDisabled = pDisabled;
    mAdditionalProperties = pAdditionalProperties;
  }

  public String getKey() {
    return mKey;
  }

  public boolean isHistorical() {
    return mHistorical;
  }

  public boolean isDisabled() {
    return mDisabled;
  }

  /**
   * Gets the string value of an "additional property" for this entry. An additional property is any element encountered
   * in the mapset "rec" definition which was not the "key" or "data". Only simple elements with string values are currently supported.
   * @param pPropertyName Element name containing the desired property string.
   * @return The string contents of the element of the given name within this mapset entry's "rec" container, or null if
   * no such element was defined.
   */
  public String getAdditionalPropertyString(String pPropertyName) {

    for(DOM lAdditionalProperty : mAdditionalProperties) {
      if(pPropertyName.equals(lAdditionalProperty.getName())) {
        return lAdditionalProperty.value();
      }
    }

    return null;
  }

  void serialiseToRecDOM(DOM pRecDOM) {
    pRecDOM.addElem(MapSet.KEY_ELEMENT_NAME, mKey);

    if(mHistorical) {
      pRecDOM.addElem(HISTORICAL, Boolean.toString(mHistorical));
    }

    if(mDisabled) {
      pRecDOM.addElem(DISABLED, Boolean.toString(mDisabled));
    }

    for(DOM lAdditionalProp : mAdditionalProperties) {
      lAdditionalProp.copyToParent(pRecDOM);
    }
  }
}
