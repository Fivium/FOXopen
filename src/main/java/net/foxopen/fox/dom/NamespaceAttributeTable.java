package net.foxopen.fox.dom;


import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;

import java.util.Collection;
import java.util.Map;

import net.foxopen.fox.module.datanode.NodeAttribute;


/**
 * Container representing attributes from a DOM node, stored based on their namespace prefix and local name. This allows
 * a consumer to perform bulk operations based on either a namespace or attribute name without having to deal with the
 * complexities of implementing such access.<br/><br/>
 *
 * Attributes not in a namespace should be accessed using empty string as the namepsace name.<br/><br/>
 *
 * The current implemention uses a Guava HashBasedTable. Note that the views returned as Collections from this object may
 * be mutable and mutate the underlying Table. To avoid this, call {@link #createImmutableCopy} at the earliest available
 * place to ensure the underlying table is 'locked' from unwanted modification.
 */
public class NamespaceAttributeTable {

  /** Namespace (row), attribute name (column), attribute value (value) */
  private final Table<String, String, String> mTable;

  /**
   * Creates a new NamespaceAttributeTable based on the attributes on the given DOM.
   * @param pDOM DOM with nameps
   * @return
   */
  static NamespaceAttributeTable createFromDOM(DOM pDOM) {

    Table<String, String, String> lTable = HashBasedTable.create();
    Map<String, String> lAttributeMap = pDOM.getAttributeMap();

    for(Map.Entry<String, String> lAttribute : lAttributeMap.entrySet()) {
      String lAttrName = lAttribute.getKey();
      String lAttrValue = lAttribute.getValue();

      // Locate namespace seperator ':'
      int lColonPosition = lAttrName.indexOf(':');

      if (lColonPosition == -1) {
        //Non-namespace attribute
        lTable.put("", lAttrName, lAttrValue);
      }
      else {
        //Split off namespace prefix
        String lAttrNS = lAttrName.substring(0,lColonPosition);
        String lAttrLocalName = lAttrName.substring(lColonPosition+1);

        lTable.put(lAttrNS, lAttrLocalName, lAttrValue);
      }
    }

    return new NamespaceAttributeTable(lTable);
  }

  private NamespaceAttributeTable(Table<String, String, String> pTable) {
    mTable = pTable;
  }

  public NamespaceAttributeTable createImmutableCopy() {
    return new NamespaceAttributeTable(ImmutableTable.copyOf(mTable));
  }

  public NamespaceAttributeTable createCopy() {
    return new NamespaceAttributeTable(HashBasedTable.create(mTable));
  }

  public void addAttribute(String pNamespace, String pAttributeName, String pAttributeValue) {
    mTable.put(pNamespace, pAttributeName, pAttributeValue);
  }

  public String getAttribute(String pNamespace, NodeAttribute pAttribute) {
    return mTable.get(pNamespace, pAttribute.getExternalString());
  }

  public String getAttribute(String pNamespace, String pAttributeName) {
    return mTable.get(pNamespace, pAttributeName);
  }

  public Collection<String> getAllAttributesForName(String pAttributeName) {
    return mTable.column(pAttributeName).values();
  }

  /**
   * Tests if the given attribute is defined in this table on any namespace.
   * @param pAttributeName
   * @return
   */
  public boolean containsAttribute(String pAttributeName) {
    return mTable.containsColumn(pAttributeName);
  }

  /**
   * Tests if the given attribute is defined in this table on any namespace.
   * @param pAttribute
   * @return
   */
  public boolean containsAttribute(NodeAttribute pAttribute) {
    return mTable.containsColumn(pAttribute.getExternalString());
  }

  public boolean containsNamespace(String pNamespace) {
    return mTable.containsRow(pNamespace);
  }

  public Map<String, String> getAttributeMapForNamespace(String pNamespace) {
    return mTable.row(pNamespace);
  }

  /**
   * Creates a copy of this AttributeTable containing only the attributes named in the given set.
   * @param pFilterAttributes Names of attributes to preserve in the new copy.
   * @return Filtered copy of this AttributeTable.
   */
  public NamespaceAttributeTable createFilteredAttributeTable(Collection<String> pFilterAttributes) {
    Table<String, String, String> lFilteredTable = HashBasedTable.create(mTable);

    //Remove all columns which do not match the attributes in the given list
    lFilteredTable.columnKeySet().retainAll(pFilterAttributes);

    return new NamespaceAttributeTable(lFilteredTable);
  }

  /**
   * Merges attributes from pSourceTable into this AttributeTable. If an attribute in pSourceTable already exists in this
   * table, it will <b>not</b> be overridden.
   * @param pSourceTable Table to merge attributes from.
   */
  public void mergeTable(NamespaceAttributeTable pSourceTable) {

    for(Table.Cell<String, String, String> lCell : pSourceTable.mTable.cellSet()){
      if(!mTable.contains(lCell.getRowKey(), lCell.getColumnKey())) {
        mTable.put(lCell.getRowKey(), lCell.getColumnKey(), lCell.getValue());
      }
    }
  }
}
