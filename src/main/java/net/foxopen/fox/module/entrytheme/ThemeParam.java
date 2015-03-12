package net.foxopen.fox.module.entrytheme;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.datanode.NodeInfo;

/**
 * Simple subclass for ease of parsing
 * and storing param definitions.
 */
public class ThemeParam {

  private String mName;
  private String mType;
  private String mXPath;
  private boolean mMand;
  private DOM mTypeDOM;

  /**
   * Parses a theme parameter from its definition XML.
   *
   * @param pDOM parameter definition xml
   * @param pModule owning module
   * @param pRootElemName the root element of the storage location document
   * @throws net.foxopen.fox.ex.ExModule invalid definition or error expanding type definitions
   */
  public ThemeParam(DOM pDOM, Mod pModule, String pRootElemName)
  throws ExModule {
    // Sanity check
    if (pDOM == null) {
      throw new ExInternal("Internal error parsing entry-theme param-list or return. Null DOM passed to ThemeParams constructor.");
    }

    // Read off attributes
    mName = pDOM.getAttrOrNull("name");
    mType = pDOM.getAttrOrNull("type");
    mXPath = pDOM.getAttrOrNull("xpath");
    String lMand = pDOM.getAttrOrNull("mand");

    // If mand is specified, it is not-null and a value other than "." is invalid
    // Assert that this is true, and then if mand is not null, we can assign a boolean to it
    if (!".".equals(lMand)) {
      throw new ExModule("Invalid entry-theme " + pDOM.getLocalName() + " definition, mand must equal '.' if specified.");
    }
    else {
      mMand = (lMand != null);
    }

    // Type provided
    if (!XFUtil.isNull(mType)) {

      // Check that name is provided
      if (XFUtil.isNull(mName)) {
        throw new ExModule("Invalid entry-theme " + pDOM.getLocalName() + " definition, type specified, but name not specified");
      }

      // We're processing an XML Schema type, no need to do any more work
      // TODO: Remove hardcoded xs prefix when DOM can deal with namespaces
      if (mType.startsWith("xs:")) {
         mTypeDOM = null;
      }

      // We're processing a complexType or user-specified simpleType parameter
      // and need a direct reference to it
      else {
        try {
          // Get module merge dom (type expanded) as this is what maps to model DOM
          DOM lModMergerTypeExpandedDOM = pModule.getModuleMergerTypeExpandedDOMOrNull();

          // Check that this wasn't cleared before we need it (should never happen)
          if (lModMergerTypeExpandedDOM == null) {
            throw new ExInternal("ModMergerDOM was null in entry-theme processing.");
          }

          StringBuilder lTypeBuffer = new StringBuilder(mType);
          String lType = null;
          String lTypeSeek = null;
          DOM lTypeDOM = null;
          DOM lTypeSeekDOM = null;

          // Pop the head of the path (if any) to support "type" patterns of COMPLEX_TYPE_OUTER/ELEMENT so
          // that the inner XML schema elements doesn't have to be defined as a separate type
          // (chain into the type hierarchy)
          lTypeSeek = XFUtil.pathPopHead(lTypeBuffer, false);
          lType = lTypeSeek;

          // TODO: Remove hardcoded xs prefix when DOM can deal with namespaces
          lTypeDOM = lModMergerTypeExpandedDOM.xpath1E("/*/*[(name(.)='xs:complexType' or name(.)='xs:simpleType') and @name='" + lTypeSeek + "']");
          lTypeSeekDOM = lTypeDOM;

          // Keep popping (if anything is left) and attempting to match xs:element definitions until we exhaust the path or error
          while (!XFUtil.isNull(lTypeSeek = XFUtil.pathPopHead(lTypeBuffer, false))) {
            // TODO: Remove hardcoded xs prefix when DOM can deal with namespaces
            lTypeSeekDOM = lTypeSeekDOM.xpath1E("./xs:sequence/xs:element[@name='" + lTypeSeek + "']");
            lTypeDOM = lTypeSeekDOM;

            // Check to see if we have a contained xs:complexType or xs:simpleType node which we'll need to drop into later
            DOMList lSchemaDOMFragChildren = lTypeDOM.xpathUL("./*[name(.)='xs:complexType' or name(.)='xs:simpleType']", null);

            // We have no complexType or simpleType child nodes, derive the type directly
            if (lSchemaDOMFragChildren.getLength() == 0) {
              // Check that there is a type attribute
              if (lTypeDOM.getAttrOrNull("type") != null) {
                lType = lTypeDOM.getAttr("type");
                break;
              }
              else {
                throw new ExModule("Invalid entry-theme " + pDOM.getLocalName() + ", schema element matching xpath '" + mXPath + "' has no type attribute and does not contain a simpleType or complexType child.");
              }
            }

            // We have exactly one complexType or simpleType definition, recurse into it if it's a complexType
            else if (lSchemaDOMFragChildren.getLength() == 1) {
              DOM lFragment = lSchemaDOMFragChildren.item(0);
              if (lFragment.getName().equals("xs:complexType")) {
                lTypeSeekDOM = lFragment;
              }
              lTypeDOM = lFragment;
            }

            lType = lTypeSeek;
          }

          // Belt and braces check
          if (XFUtil.isNull(lType)) {
            throw new ExInternal("Failed to correctly process type/element path '" + mType + "'");
          }

          // Reference DOM in theme
          mTypeDOM = lTypeDOM;
          mType = lType;
        }
        catch (ExCardinality ex) {
          throw new ExModule("Invalid entry-theme " + pDOM.getLocalName() + " definition, xs:complexType with name '" + mType + "' not found.", ex);
        }
        catch (ExBadPath ex) {
          throw new ExModule("Invalid entry-theme " + pDOM.getLocalName() + ", invalid type name '" + mType + "'.", ex);
        }
      }
    }

    // Type not provided, but XPath provided
    else if (!XFUtil.isNull(mXPath)) {

      // We're stateless, so XPaths must be relative to the root
      if (mXPath.indexOf(":{") != -1) {
        throw new ExModule("");
      }

      try {
        // Create the target element by xpath
        DOM lDOM = DOM.createDocument(pRootElemName);
        DOMList lDOMList = lDOM.getCreateXPathUL(mXPath);

        if (lDOMList.getLength() != 1) {
          throw new ExInternal("Error creating target element in entry-theme xpath evaluation");
        }

        // Get schema data from the model DOM
        NodeInfo lNodeInfo = pModule.getNodeInfo(lDOMList.item(0));

        // Check that we have something, otherwise XPath or user error
        if (lNodeInfo == null) {
          throw new ExModule("Invalid entry-theme " + pDOM.getLocalName() + ", unable to derive a type definition from XPath '" + mXPath + "', check schema definition contains the element required.");
        }

        // Get the element name from the model DOM info
        mName = lNodeInfo.getName();

        // Get the schema fragment
        DOM lSchemaDOMFragment = lNodeInfo.getParseDOM();

        // Seek children that we care about
        // TODO: Remove hardcoded xs prefix when DOM can deal with namespaces
        DOMList lSchemaDOMFragChildren = lSchemaDOMFragment.xpathUL("./*[name(.)='xs:complexType' or name(.)='xs:simpleType']", null);

        // We have no complexType or simpleType child nodes, derive the type directly
        if (lSchemaDOMFragChildren.getLength() == 0) {
          // Check that there is a type attribute
          if (lSchemaDOMFragment.getAttrOrNull("type") != null) {
            mType = lSchemaDOMFragment.getAttr("type");
          }
          else {
            throw new ExModule("Invalid entry-theme " + pDOM.getLocalName() + ", schema element matching xpath '" + mXPath + "' has no type attribute and does not contain a simpleType or complexType child.");
          }
        }

        // We have exactly one complexType or simpleType definition
        else if (lSchemaDOMFragChildren.getLength() == 1) {
          // Clone node to a standalone type and assign it the element name
          mTypeDOM = lSchemaDOMFragChildren.item(0);
          mType = mName;
        }

        else {
          throw new ExModule("Invalid schema element with name '" + mName + "', more than one xs:complexType or xs:simpleType child nodes found.");
        }
      }
      catch (ExBadPath ex) {
        throw new ExInternal("Invalid internal xpath when constructing type definition in entry-theme param or return.", ex);
      }
    }

    // Throw if both are null
    else {
      throw new ExModule("Invalid entry-theme " + pDOM.getLocalName() + ", neither type nor xpath specified.");
    }
  }

  public String getName() {
    return mName;
  }

  public String getType() {
    return mType;
  }

  public String getXPath() {
    return mXPath;
  }

  public boolean isMand() {
    return mMand;
  }

  public boolean hasTypeDOM() {
    return (mTypeDOM != null);
  }

  /**
   * Gets the type definition in an editable schema instance.
   * @return type DOM
   */
  public DOM getTypeDOM() {
    return mTypeDOM;
  }
}
