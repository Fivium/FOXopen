package net.foxopen.fox.module.serialiser.layout;

import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.serialiser.OutputSerialiser;
import net.foxopen.fox.module.serialiser.layout.items.LayoutItem;
import net.foxopen.fox.module.serialiser.layout.methods.FormLayout;
import net.foxopen.fox.module.serialiser.layout.methods.LayoutMethod;
import net.foxopen.fox.module.serialiser.layout.methods.TestLayout;
import net.foxopen.fox.track.Track;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GridLayoutManager {
  private LayoutResult mLayoutResult;

  static final Map<String, LayoutMethod> LAYOUT_METHODS = new HashMap<>();
  static {
    LAYOUT_METHODS.put("test", TestLayout.getInstance());
    LAYOUT_METHODS.put("form", FormLayout.getInstance());
  }

  /**
   * Construct a GridLayout using a LayoutMethod specified by the layoutMethod attribute on pEvalNode. The LayoutMethod
   * returns a LayoutResult containing a flat list of row start/end and columns based on the child nodes under pEvalNode
   *
   * @param pFormColumns Constraint of how many logical columns a LayoutMethod should have per-row
   * @param pSerialiser Serialiser provides the correct WidgetBuilders for child nodes of pEvalNode so it can determine what columns may be needed
   * @param pEvalNodeInfo Container node with children that need to be laid out
   */
  public GridLayoutManager(int pFormColumns, OutputSerialiser pSerialiser, EvaluatedNodeInfo pEvalNodeInfo) {
    this(pFormColumns, pSerialiser, pEvalNodeInfo, null);
  }

  /**
   * Construct a GridLayout with a given LayoutMethod. The LayoutMethod returns a LayoutResult containing a flat list of
   * row start/end and columns based on the child nodes under pEvalNode and given a pFormColumns constraint.
   *
   * @param pFormColumns Constraint of how many logical columns a LayoutMethod should have per-row
   * @param pSerialiser Serialiser provides the correct WidgetBuilders for child nodes of pEvalNode so it can determine what columns may be needed
   * @param pEvalNodeInfo Container node with children that need to be laid out
   * @param pLayoutMethod LayoutMethod class to perform the layout
   */
  public GridLayoutManager(int pFormColumns, OutputSerialiser pSerialiser, EvaluatedNodeInfo pEvalNodeInfo, LayoutMethod pLayoutMethod) {
    LayoutMethod lLayoutMethod = pLayoutMethod;
    if (lLayoutMethod == null) {
      String lLayoutMethodAttr = pEvalNodeInfo.getStringAttribute(NodeAttribute.LAYOUT_METHOD, "form");

      lLayoutMethod = LAYOUT_METHODS.get(lLayoutMethodAttr);
      if (lLayoutMethod == null) {
        throw new ExInternal("Unknown layoutMethod: " + lLayoutMethodAttr);
      }
    }

    Track.pushDebug("GridLayout", "Laying out a grid using the " + lLayoutMethod.getClass().getName() + " layout manager");
    try {
      mLayoutResult = lLayoutMethod.doLayout(pFormColumns, pSerialiser, pEvalNodeInfo);
    }
    finally {
      Track.pop("GridLayout");
    }
  }

  /**
   * Get a flat list of the LayoutItems, such as row start/end and columns, to be serialised out
   *
   * @return List of LayoutItems
   */
  public List<LayoutItem> getLayoutItems() {
    return mLayoutResult.getLayoutItems();
  }

  /**
   * Get a count of the Rows the LayoutMethod created for the given columns
   *
   * @return Count of Row Start LayoutItems
   */
  public int getRowCount() {
    return mLayoutResult.getRowCount();
  }

  /**
   * Get a count of all the columns the LayoutMethod created that are not filler columns. This includes both widget fields
   * and prompts too.
   *
   * @return Count of non-filler column items (not cumulative colspan)
   */
  public int getFilledColumnCount() {
    return mLayoutResult.getFilledColumnCount();
  }

  /**
   * Get a count of all the columns the LayoutMethod created that have a prompt in them
   *
   * @return Count of prompt column items (not cumulative colspan)
   */
  public int getPromptColumnCount() {
    return mLayoutResult.getPromptColumnCount();
  }

  /**
   * Get a count of all the columns the LayoutMethod created that have a widget in them
   *
   * @return Count of widget column items (not cumulative colspan)
   */
  public int getWidgetColumnCount() {
    return mLayoutResult.getWidgetColumnCount();
  }

  /**
   * Get a count of all the columns the LayoutMethod created that are just filler columns to pad out a row
   *
   * @return Count of filler column items (not cumulative colspan)
   */
  public int getFillerColumnCount() {
    return mLayoutResult.getFillerColumnCount();
  }
}
