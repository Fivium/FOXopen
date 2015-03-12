/*

Copyright (c) 2010, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE -
                    ENERGY DEVELOPMENT UNIT (IT UNIT)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the DEPARTMENT OF ENERGY AND CLIMATE CHANGE nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

$Id$

*/
package net.foxopen.fox.module.datanode;


import net.foxopen.fox.FoxGbl;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.dom.NamespaceAttributeTable;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.module.Mod;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;


public class NodeInfo {
  public static final String FOX_NAMESPACE = "fox";

  static final int LIST_HIGH_VALUE = Integer.MAX_VALUE;
  static final int LIST_NO_VALUE = -1;

  private static final Set<String> SUPPORTED_XS_DATATYPES = new HashSet<>();
  private static final Set<String> SUPPORTED_XS_FACETS = new HashSet<>();
  private static final Integer DATE_LENGTH = Integer.valueOf(10);
  private static final Integer DATE_TIME_LENGTH = Integer.valueOf(20);

  private final String mAbsolutePath;
  private final NodeInfo mParentNodeInfo;

  private String mXMLSchemaDataType = "";

  private final String mName;

  /** whether this element has a collection of different type of children (different types of child elements)  */
  private boolean mContainsCollection;
  /** whether this element has children of all the same type  */
  private boolean mContainsList;
  /** a pointer to this nodes location in the model dom */
  private DOM mModelDom;
  /** holds a reference to the parent class that created this nodeinfo */
  private Mod mParentModule;
  /** holds the elements attributes namespace values as the key and contains another hashtable with the attribute names linked to the attribute values */
  private final NamespaceAttributeTable mNamespaceAttributeTable;

  /** the maximum length for this piece of data **/
  private Integer f_maxDataLength = Integer.MAX_VALUE; // This is meant to be the maximum value to force the developer to use the correct length
  /** the minimum length for this piece of data **/
  private Integer f_minDataLength = Integer.valueOf(0); // default for when not specified on schema

  // xs facet attributes see xml schema doc for definitive definition
  private Integer f_totalDigits;
  private Integer f_fractionDigits;
  private Integer f_length;
  private Double f_minInclusive;
  private Double f_maxInclusive;
  private Double f_minExclusive;
  private Double f_maxExclusive;

  private int minOccurs = 1; // XMLSchema default is 1 when not specified
  private int maxOccurs = 1;

  private List<String> mSchemaEnumeration;
  private int mEnumerationMaxLength = 1;

  private List<Pattern> mSchemaPattern;

  private DOM mParseDOM;

  private final Map<String, NamespaceAttributeTable> mCellmateAttributeGroups = new HashMap<>();

  static {
    // If you have a new data type to be supported add it in here
    // you will also need to add code into set xsType or SetDatatype accordingly
    // for validation look up validate class
    // base data types
    SUPPORTED_XS_DATATYPES.add ("xs:string");
    SUPPORTED_XS_DATATYPES.add ("xs:decimal");
    SUPPORTED_XS_DATATYPES.add ("xs:date");
    SUPPORTED_XS_DATATYPES.add ("xs:long");
    SUPPORTED_XS_DATATYPES.add ("xs:int");
    SUPPORTED_XS_DATATYPES.add ("xs:dateTime");
    SUPPORTED_XS_DATATYPES.add ("xs:time");
    SUPPORTED_XS_DATATYPES.add ("xs:boolean");
    SUPPORTED_XS_DATATYPES.add ("xs:positiveInteger");
    SUPPORTED_XS_DATATYPES.add ("xs:negativeInteger");
    SUPPORTED_XS_DATATYPES.add ("xs:integer");
    SUPPORTED_XS_DATATYPES.add ("xs:anyType");
    SUPPORTED_XS_DATATYPES.add("file-type");

    // data type attributes (or facets)
    SUPPORTED_XS_FACETS.add ("xs:totalDigits");
    SUPPORTED_XS_FACETS.add ("xs:fractionDigits");
    SUPPORTED_XS_FACETS.add ("xs:length");
    SUPPORTED_XS_FACETS.add ("xs:maxLength");
    SUPPORTED_XS_FACETS.add ("xs:minLength");
    SUPPORTED_XS_FACETS.add ("xs:minInclusive");
    SUPPORTED_XS_FACETS.add ("xs:maxInclusive");
    SUPPORTED_XS_FACETS.add ("xs:minExclusive");
    SUPPORTED_XS_FACETS.add ("xs:maxExclusive");
  }

  /**
   * Test if NodeInfo has pDataType in its list of supported XS Datatypes
   *
   * @param pDatatype Datatype to test
   * @return True if pDatatype is in the list of supported XS Datatypes
   */
  public static boolean containsSupportedXsDatatype(String pDatatype) {
    return SUPPORTED_XS_DATATYPES.contains(pDatatype);
  }

  /**
   * Test if NodeInfo has pFacet in its list of supported XS Facets
   *
   * @param pFacet Datatype to test
   * @return True if pFacet is in the list of supported XS Facets
   */
  public static boolean containsSupportedXsFacet(String pFacet) {
    return SUPPORTED_XS_FACETS.contains(pFacet);
  }

  /**
   * @param name The name for this node
   * @param xml_type The type of node that this is, for example NodeInfo.XML_ELEMENT
   * @param pParentNodeInfo The NodeInfo for the parent of this node
   * @param absolute The xpath value for this node
   */
  /**
   * Construct a new NodeInfo
   *
   * @param pParseDOM The schema DOM for the current module
   * @param pElementName The name of the element this NodeInfo represents
   * @param pModelDOM The Model DOM for the current Module
   * @param pParentNodeInfo The NodeInfo for the parent of this node
   * @param pAbsolutePath Absolute path to this NodeInfo in the Model DOM
   * @param pModule The module this NodeInfo was created by/for
   * @throws ExInternal
   * @throws ExModule
   */
  public NodeInfo(
    DOM pParseDOM
  , String pElementName
  , DOM pModelDOM
  , NodeInfo pParentNodeInfo
  , String pAbsolutePath
  , Mod pModule
  )
  throws
    ExInternal
  , ExModule
  {
    mName = pElementName;

    mParentModule = pModule;
    mAbsolutePath = pAbsolutePath;
    mParentNodeInfo = pParentNodeInfo;
    mModelDom = pModelDOM;
    mParseDOM = pParseDOM;

    setContainsCollection(false);
    setContainsList(false);

    boolean listMember = false;

    NamespaceAttributeTable lAttributeTable = pParseDOM.getNamespaceAttributeTable();

    // Look on element definition for:
    //   XMLSchema attributes (leaving behind)
    //   mode:feature="conditional/value" attributes (removing when passed)
    //   fox:feature="value" attributes (removing when passed)

    //Process attributes with special meanings
    String lMinOccurs = lAttributeTable.getAttribute("", "minOccurs");
    if(lMinOccurs != null) {
      setMinCardinality(Integer.parseInt(lMinOccurs));
    }

    String lMaxOccurs = lAttributeTable.getAttribute("", "maxOccurs");
    if(lMaxOccurs != null) {
      maxChild(lMaxOccurs, lAttributeTable);
      setMaxCardinality("unbounded".equals(lMaxOccurs) ? Integer.MAX_VALUE : Integer.parseInt(lMaxOccurs));
      listMember = true;
    }

    String lType = lAttributeTable.getAttribute("", "type");
    if(lType != null) {
      setDataType(lType);
    }

    //Validate action names
    for(String lActionName : lAttributeTable.getAllAttributesForName("action")) {
      if(pModule.badActionName(lActionName)) {
        pModule.addBulkModuleErrorMessage("\nBad action name defined on element " +pAbsolutePath+": "+lActionName);
      }
    }

    //If this element has a mixed attribute on its child complexType, merge it up to this nodeinfo
    mixedTypeChild(pParseDOM, lAttributeTable);

    mNamespaceAttributeTable = lAttributeTable.createImmutableCopy();

    if (!listMember && mParentNodeInfo != null) {
      mParentNodeInfo.setContainsCollection(true);
    }
  }

  /**
   * Returns the name of the element.
   */
  public String getName() {
    return mName;
  }

  /**
   * Returns the minimum cardinality of this node within the scope of its parent.
   *
   * @return the nodes mimimum cardinality
   */
   public int getMinCardinality()
   {
      return minOccurs;
   }

  /**
   * Sets the minimum cardinality of this node within the scope of its parent.
   *
   * @return the nodes mimimum cardinality
   */
   public void setMinCardinality(int newValue)
   {
      minOccurs = newValue;
   }

  /**
   * Returns the maximum cardinality of this node within the scope of its parent.
   *
   * @return the nodes maximum cardinality
   */
   public int getMaxCardinality()
   {
      return maxOccurs;
   }

  /**
   * Sets the maximum cardinality of this node within the scope of its parent.
   *
   * @return the nodes maximum cardinality
   */
   public void setMaxCardinality(int newValue)
   {
      maxOccurs = newValue;
   }

  /**
   * Gets an attribute from the "fox" namespace, or null if it's not defined.
   * @param pAttribute Attribute to retrieve.
   * @return Attribute value or null.
   */
  public String getFoxNamespaceAttribute(NodeAttribute pAttribute) {
    return mNamespaceAttributeTable.getAttribute(FOX_NAMESPACE, pAttribute.getExternalString());
  }

  public String getAttribute(String pNamespace, NodeAttribute pAttribute) {
    return mNamespaceAttributeTable.getAttribute(pNamespace, pAttribute.getExternalString());
  }

  /**
   * Gets an elements attribute from the element NodeInfo, returns null if it doesn't exist
   */
  public String getAttribute(String pNamespace, String pAttribute) {
    return mNamespaceAttributeTable.getAttribute(pNamespace, pAttribute);
  }

  /**
   * A test to see if this namespace exists in this NodeInfo
   */
  public boolean getNamespaceExists(String pNamespace) {
    return mNamespaceAttributeTable.containsNamespace(pNamespace);
  }


  /**
   *  Returns all the attributes found on the element mapped as Namespace => {Attribute => Value}
   */
  public NamespaceAttributeTable getNamespaceAttributeTable() {
    return mNamespaceAttributeTable;
  }
  /**
   * Set that this node has children which contain a list
   */
  public void setContainsList(boolean pList) {
    mContainsList = pList;
  }

  /**
   * See if this node has children which contains a list
   */
  public boolean isListContainer() {
    return mContainsList;
  }

  /**
   * See if this node has children which contains a collection
   */
  public boolean getContainsCollection() {
    return mContainsCollection;
  }

  public void setContainsCollection(boolean cColl) {
    mContainsCollection = cColl;
  }

  /**
   * Returns whether the element is an item of a collection
   */
  public boolean getIsItem() {
    return (!getContainsCollection() && !isListContainer());
  }

  /**
   * Return true if the current node is a complex list type marked up either as a multi-selector or a file storage location
   *
   * @return If this NodeInfo is a selector or file storage location item
   */
  public boolean isMultiOptionItem() {
    return isMultiSelectorItem() || isMultiUploadItem();
  }

  /**
   * IS this item marked up as a multi upload container
   *
   * @return
   */
  public boolean isMultiUploadItem() {
    return getNamespaceAttributeTable().containsAttribute(NodeAttribute.FILE_STORAGE_LOCATION);
  }

  /**
   * Is this item marked up as a multi-selector
   *
   * @return
   */
  public boolean isMultiSelectorItem() {
    return getNamespaceAttributeTable().containsAttribute(NodeAttribute.SELECTOR);
  }

  /**
   * Gets the element for this nodeInfo from the model dom
   */
  public DOM getModelDOMElem() {
    return mModelDom;
  }

  /**
   * Sets the maximum number of children that this nodes parent can have of this type.
   */
  private void maxChild(String pMaxOccurs, NamespaceAttributeTable pNamespaceAttributeTable) {
    if ("unbounded".equals(pMaxOccurs)) {
      pNamespaceAttributeTable.addAttribute("", "maxOccurs", String.valueOf(Integer.MAX_VALUE));
    }
    else {
      pNamespaceAttributeTable.addAttribute("", "maxOccurs", pMaxOccurs);
    }

    //Tell the parent it's a list. Previously this skipped making nodes with a selector attribute a list
    if (mParentNodeInfo != null) {
      mParentNodeInfo.setContainsList(true);
    }
  }

  private void mixedTypeChild(DOM pParseDOM, NamespaceAttributeTable pNamespaceAttributeTable) {
    DOM lDefinitionComplexTypeChild = null;
    try {
      lDefinitionComplexTypeChild = pParseDOM.get1E("xs:complexType");

      String lMixed = pParseDOM.getAttrOrNull("mixed");
      if(lMixed != null && ("1".equals(lMixed) || "true".equals(lMixed.toLowerCase()))) {
        pNamespaceAttributeTable.addAttribute("", "mixed", "true");
      }
    }
    catch (ExTooFew | ExTooMany e) {
      //Not a problem
    }
  }

  /**
   * Get the type of node that this presents, e.g. XML_ELEMENT
   */
  public NodeType getNodeType() {
    if(this.getContainsCollection()) {
      return NodeType.COLLECTION;
    }
    else if(this.isListContainer()) {
      return NodeType.LIST;
    }
    else {
      return NodeType.ITEM;
    }
  }

  public void setMaxDataLength(Integer pMaxLength) {
    f_maxDataLength = pMaxLength;
  }

  public Integer getMaxDataLength() {
    if (f_totalDigits == null) {
      if (f_maxDataLength == null && f_length != null) {
        return f_length;  // Default max length to length if there is one
      }
      else {
        return f_maxDataLength;
      }
    }
    else {
      if (f_fractionDigits == null) {
        return f_totalDigits;
      }
      else {
        return Integer.valueOf(f_totalDigits.intValue() + 1);
      }
    }
  }

  /**
  * Gets the equivalent xs attribute defined in the module of the same name
  *
  **/
  public Integer getMinDataLength() {
    return f_minDataLength;
  }

  /**
  * Gets the equivalent xs attribute defined in the module of the same name
  *
  **/
  public Integer getTotalDigits() {
    return f_totalDigits;
  }

  /**
  * Gets the equivalent xs attribute defined in the module of the same name
  *
  **/
  public Integer getFractionDigits() {
    return f_fractionDigits;
  }

  /**
  * Gets the equivalent xs attribute defined in the module of the same name
  *
  **/
  public Integer getLength() {
    return f_length;
  }

  /**
  * Gets the equivalent xs attribute defined in the module of the same name
  *
  **/
  public Double getMinInclusive() {
    return f_minInclusive;
  }

  /**
  * Gets the equivalent xs attribute defined in the module of the same name
  *
  **/
  public Double getMaxInclusive() {
    return f_maxInclusive;
  }

  /**
  * Gets the equivalent xs attribute defined in the module of the same name
  *
  **/
  public Double getMinExclusive() {
    return f_minExclusive;
  }

  /**
  * Gets the equivalent xs attribute defined in the module of the same name
  *
  **/
  public Double getMaxExclusive() {
    return f_maxExclusive;
  }

  //PN TODO make this an enum
  public final void setDataType(String pSchemaType) {
    if (mXMLSchemaDataType.length() != 0) {
      // override datatypes as its valid for a restriction to override the base type but log it
      //Logging.sysLogInfo("Overiding "+mName+"'s xs datatype from "+mXMLSchemaDataTypeIntern + " to "+pSchemaType);
    }

    mXMLSchemaDataType = pSchemaType;

    if("xs:date".equals(mXMLSchemaDataType)) {
      f_maxDataLength = DATE_LENGTH;
//      f_hintTextPlus = "Please enter the date in this format "+FoxGbl.FOX_DATE_DISPLAY_FORMAT+"<br/>e.g."+FoxGbl.FOX_DATE_DISPLAY_FORMAT_EG;
    }
    else if("xs:dateTime".equals(mXMLSchemaDataType)) {
      f_maxDataLength = DATE_TIME_LENGTH;
//      f_hintTextPlus = "Please enter the date time in this format "+FoxGbl.FOX_DATE_TIME_DISPLAY_FORMAT+"<br/>e.g."+FoxGbl.FOX_DATE_TIME_DISPLAY_FORMAT_EG;
    }
  }

  /**
  * Adds an item to the boundary variable (min/max between etc) and checks its data type
  *
  **/
  private final Double addBounds (String pBoundaryVal)
  throws ExModule {
    Double returnVal;
    if ("date".equals(mXMLSchemaDataType)) {
      try {
        DateFormat foxFormat = new SimpleDateFormat(FoxGbl.FOX_JAVA_DATE_TIME_FORMAT);
        Date itemVal = foxFormat.parse(pBoundaryVal);
        // add date in as a double as it makes it easy to compare times
        returnVal = new Double (itemVal.getTime()) ;
      }
      catch (Exception ex) {
        throw new ExModule ("Boundary date ("+pBoundaryVal+" not in expected fox format "+ FoxGbl.FOX_JAVA_DATE_TIME_FORMAT  , ex);
      }
    }
    else {
      returnVal =  new Double(pBoundaryVal);
    }
    return returnVal;
  }

  /**
  * Sets a XS facet data structure with the appropriate value (e.g.xs:minInclusive = 1)
  * Does minor datatype validation on the bounds check values and ennumration (valid values) items
  **/
  public final void setXsFacet (String pXsType, String pValue)
  throws ExModule {
    try {
      if (SUPPORTED_XS_FACETS.contains(pXsType)) {
        if (pXsType.equals("xs:totalDigits")) {
          f_totalDigits = Integer.valueOf(pValue);
        }
        else if ( pXsType.equals("xs:fractionDigits") ) {
          f_fractionDigits = Integer.valueOf(pValue);
        }
        else if ( pXsType.equals("xs:length") ) {
          f_length = Integer.valueOf(pValue);
        }
        else if ( pXsType.equals("xs:minLength") ) {
          f_minDataLength = Integer.valueOf(pValue);
        }
        else if ( pXsType.equals("xs:maxLength") ) {
          f_maxDataLength = Integer.valueOf(pValue);
        }
        else if ( pXsType.equals("xs:minInclusive") ) {
          f_minInclusive = addBounds ( pValue);
        }
        else if ( pXsType.equals("xs:maxInclusive") ) {
          f_maxInclusive = addBounds ( pValue);
        }
        else if ( pXsType.equals("xs:minExclusive") ) {
          f_minExclusive = addBounds ( pValue);
        }
        else if ( pXsType.equals("xs:maxExclusive") ) {
          f_maxExclusive = addBounds ( pValue);
        }

      }
    }
    catch (ExModule ex) {
      throw new ExModule ("Invalid "+ pXsType + " value " + pValue + " for parent datatype " + mXMLSchemaDataType  , ex);
    }
    catch (NumberFormatException ex) {
      throw new ExModule ("Invalid numeric "+ pXsType + " value " + pValue + " for parent datatype " + mXMLSchemaDataType  , ex);
    }
  }

  public final String getDataType() {
    return mXMLSchemaDataType;
  }

  public final void addSchemaPattern(Pattern pPattern) {
    if(mSchemaPattern == null) {
      mSchemaPattern = new ArrayList<>();
    }
    mSchemaPattern.add(pPattern);
  }

  public final List<Pattern> getSchemaPatternOrNull() {
    return mSchemaPattern;
  }

  public final void addSchemaEnumeration(String pValue) {
    if(mSchemaEnumeration == null) {
      mSchemaEnumeration = new ArrayList<>();
    }

    if (pValue.length() > mEnumerationMaxLength) {
      mEnumerationMaxLength = pValue.length();
    }

    mSchemaEnumeration.add(pValue);
  }

  public final List<String> getSchemaEnumerationOrNull() {
    return mSchemaEnumeration;
  }

  public final Integer getSchemaEnumerationMaxLength(){
    return mEnumerationMaxLength;
  }

  public DOM getParseDOM () {
    return mParseDOM;
  }

  public Mod getParentModule() {
    return mParentModule;
  }

  public void addCellmateAttributes(String pCellmateKey, DOM pSequenceDOM)
  throws ExModule {
    //Check for duplicate cellmate key definitions
    if(mCellmateAttributeGroups.containsKey(pCellmateKey)) {
      throw new ExModule("Duplicate cellmateKey definition: " + pCellmateKey);
    }

    NamespaceAttributeTable lAttributeTable = pSequenceDOM.getNamespaceAttributeTable();
    mCellmateAttributeGroups.put(pCellmateKey, lAttributeTable);
  }

  public NamespaceAttributeTable getCellmateAttributes(String pCellmateKey) {
    return mCellmateAttributeGroups.get(pCellmateKey);
  }

  public NodeInfo getParentNodeInfo() {
    return mParentNodeInfo;
  }

  public String getAbsolutePath() {
    return mAbsolutePath;
  }


  /**
   * Gets the max cardinality for this NodeInfo, if it is a list container.
   * @return The maximum cardinality ("maxOccurs") attribute of this list container's repeating item, or 0 if this is not
   * a list container.
   */
  public int getListMaxCardinality() {
    DOMList lModelChildNodes = mModelDom.getChildNodes();
    if(lModelChildNodes.size() == 1) {
      //TODO PN this can be improved when NodeInfos know about their children
      return mParentModule.getNodeInfo(lModelChildNodes.get(0)).getMaxCardinality();
    }
    else if(lModelChildNodes.size() == 0) {
      return 0;
    }
    else {
      throw new ExInternal("List container cannot have more than one child element at " + getAbsolutePath());
    }
  }
}
