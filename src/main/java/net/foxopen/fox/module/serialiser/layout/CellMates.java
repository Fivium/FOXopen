package net.foxopen.fox.module.serialiser.layout;

import net.foxopen.fox.dom.NamespaceAttributeTable;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoCellMateCollection;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.datanode.NodeEvaluationContext;
import net.foxopen.fox.module.evaluatedattributeresult.FixedStringAttributeResult;
import net.foxopen.fox.module.evaluatedattributeresult.StringAttributeResult;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;

import java.util.HashSet;
import java.util.Set;

/**
 * The CellMates class is a type of CellItem which is itself a container for regular CellItems. Cellmates are defined
 * as an xs:sequence in the module and at module parse time their attributes are tied to a
 *
 * This item is also referred to as the "Jail" as it is the container of the CellMates and goes along with the analogy.
 */
public class CellMates extends CellItem {
  private final String mCellmateKey;
  private final EvaluatedNodeInfoCellMateCollection mCellMatesContainer;

  private final StringAttributeResult mJailPrompt;

  private final CellItem.Dimensions mDimensions = new CellItem.Dimensions();

  private final Set<EvaluatedNode> mCellMates = new HashSet<>();

  public CellMates(int pColumnLimit, WidgetBuilder pItemWidgetBuilder, String pCellmateKey, EvaluatedNode pRootCellMate) {
    super(pRootCellMate);

    // Set jail members from root
    mCellmateKey = pCellmateKey;

    NamespaceAttributeTable lCellmatesAttributes = ((EvaluatedNodeInfo)pRootCellMate.getParent()).getNodeInfo().getCellmateAttributes(pCellmateKey);

    // Create a enw NodeEvaluationContext from the root cell mate's but with the cellmate attributes passed in
    NodeEvaluationContext lStubNEC = pRootCellMate.getNodeEvaluationContext();
    NodeEvaluationContext lCellMatesNodeEvaluationContext = NodeEvaluationContext.createNodeInfoEvaluationContext(lStubNEC.getEvaluatedParseTree(), pRootCellMate.getEvaluatedPresentationNode(),
      lStubNEC.getDataItem(), lStubNEC.getEvaluateContextRuleItem(),
      null, lCellmatesAttributes, null, lStubNEC);

    mCellMatesContainer = new EvaluatedNodeInfoCellMateCollection(this, pRootCellMate.getParent(), pRootCellMate.getEvaluatedPresentationNode(), lCellMatesNodeEvaluationContext, pRootCellMate.getVisibility(), ((EvaluatedNodeInfo)pRootCellMate).getNodeInfo());

    // Get the prompt for the Jail
    StringAttributeResult lCellmatePrompt = lCellMatesNodeEvaluationContext.getStringAttributeOrNull(NodeAttribute.PROMPT);
    if (lCellmatePrompt != null) {
      mJailPrompt = lCellmatePrompt;
    }
    else {
      mJailPrompt = new FixedStringAttributeResult("");
    }

    // Calculate prompt span
    //TODO - NP - support attributes in filtered namespace list
    StringAttributeResult lJailPromptSpanAttributeOrNull = lCellMatesNodeEvaluationContext.getStringAttributeOrNull(NodeAttribute.PROMPT_SPAN);
    if (lJailPromptSpanAttributeOrNull != null) {
      mDimensions.promptSpan = Math.min(pColumnLimit, Integer.valueOf(lJailPromptSpanAttributeOrNull.getString()));
    }
    else {
      throw new ExInternal("Cellmates require a promptSpan for the prompt of the jail");
    }

    // Calculate field span
    StringAttributeResult lJailFieldSpanAttributeOrNull = lCellMatesNodeEvaluationContext.getStringAttributeOrNull(NodeAttribute.FIELD_SPAN);
    if (lJailFieldSpanAttributeOrNull != null) {
      mDimensions.fieldSpan = Math.min(pColumnLimit, Integer.valueOf(lJailFieldSpanAttributeOrNull.getString()));
    }
    else {
      throw new ExInternal("Missing a jailFieldSpan on the cellmates root element");
    }

    // Calculate offset span
    StringAttributeResult lOffsetSpan = lCellMatesNodeEvaluationContext.getStringAttributeOrNull(NodeAttribute.OFFSET_SPAN);
    if (lOffsetSpan != null) {
      mDimensions.offsetSpan = Math.min(pColumnLimit, Integer.parseInt(lOffsetSpan.getString()));
    }
  }

  public String getCellmateKey() {
    return mCellmateKey;
  }

  public void addCellMate(EvaluatedNodeInfo mCellMate) {
    mCellMates.add(mCellMate);
    mCellMatesContainer.addChild(mCellMate);
  }

  @Override
  public boolean isIndividualCell() {
    return false;
  }

  @Override
  public CellItem.Dimensions getDimensions() {
    return mDimensions;
  }

  @Override
  public String getPromptLayout() {
    return "west";
  }

  @Override
  public String toString() {
    return mCellmateKey + "(" + mCellMates.size() + ")";
  }

  @Override
  public EvaluatedNode getCellItem() {
    return mCellMatesContainer;
  }

  public final StringAttributeResult getJailPrompt() {
    return mJailPrompt;
  }

  @Override
  public String getDisplayBeforeAttribute() {
    return mCellMatesContainer.getDisplayBeforeAttribute();
  }

  @Override
  public String getDisplayAfterAttribute() {
    return mCellMatesContainer.getDisplayAfterAttribute();
  }

  @Override
  public String getDisplayOrder() {
    return mCellMatesContainer.getDisplayOrder();
  }

  @Override
  public String getName() {
    return mCellMatesContainer.getName();
  }
}
