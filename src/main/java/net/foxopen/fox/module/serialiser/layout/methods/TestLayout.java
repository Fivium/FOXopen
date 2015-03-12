package net.foxopen.fox.module.serialiser.layout.methods;

import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.datanode.NodeVisibility;
import net.foxopen.fox.module.serialiser.OutputSerialiser;
import net.foxopen.fox.module.serialiser.layout.LayoutResult;
import net.foxopen.fox.module.serialiser.layout.items.LayoutItem;
import net.foxopen.fox.module.serialiser.layout.items.LayoutItemRowEnd;
import net.foxopen.fox.module.serialiser.layout.items.LayoutItemRowStart;
import net.foxopen.fox.module.serialiser.layout.items.LayoutWidgetItemColumn;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple LayoutMethod implementation to test the concepts work.
 * This lays out all widgets on their own row, full colspan, and if there's a prompt that has a full row itself before the widget.
 */
public class TestLayout implements LayoutMethod {
  private static final LayoutMethod INSTANCE = new TestLayout();

  public static final LayoutMethod getInstance() {
    return INSTANCE;
  }

  private TestLayout() {
  }

  public LayoutResult doLayout(int pColumLimit, OutputSerialiser pSerialiser, EvaluatedNodeInfo pEvalNode) {
    final List<LayoutItem> lItems = new ArrayList<>();

    int lRowCount = 0;
    int lFilledColumnCount = 0;
    int lPromptColumnCount = 0;
    int lWidgetColumnCount = 0;

    for (EvaluatedNodeInfo lItemNodeInfo : pEvalNode.getChildren()){
      if (lItemNodeInfo.getVisibility() == NodeVisibility.DENIED) {
        continue;
      }

      WidgetBuilder lItemWidgetBuilder = pSerialiser.getWidgetBuilder(lItemNodeInfo.getWidgetBuilderType());

      lItems.add(new LayoutItemRowStart());
      lRowCount++;

      if (lItemWidgetBuilder.hasPrompt(pEvalNode)) {
        LayoutWidgetItemColumn lPromptCol = new LayoutWidgetItemColumn(pColumLimit, lItemNodeInfo, true, lItemWidgetBuilder);
        lItems.add(lPromptCol);
        lPromptColumnCount++;
        lFilledColumnCount++;

        lItems.add(new LayoutItemRowEnd());
        lItems.add(new LayoutItemRowStart());
        lRowCount++;
      }

      LayoutWidgetItemColumn lWidgetCol = new LayoutWidgetItemColumn(pColumLimit, lItemNodeInfo, false, lItemWidgetBuilder);
      lItems.add(lWidgetCol);
      lWidgetColumnCount++;
      lFilledColumnCount++;

      lItems.add(new LayoutItemRowEnd());
    }

    final int lFinalRowCount = lRowCount;
    final int lFinalFiledColumnCount = lFilledColumnCount;
    final int lFinalPromptColumnCount = lPromptColumnCount;
    final int lFinalWidgetColumnCount = lWidgetColumnCount;
    final int lFinalFillerColumnCount = 0; // No filler columns are added with this LayoutMethod, all prompts/widgets are full-row

    return new LayoutResult(){
      public List<LayoutItem> getLayoutItems() { return lItems; }
      public int getRowCount() { return lFinalRowCount; }
      public int getFilledColumnCount() { return lFinalFiledColumnCount; }
      public int getPromptColumnCount() { return lFinalPromptColumnCount; }
      public int getWidgetColumnCount() { return lFinalWidgetColumnCount; }
      public int getFillerColumnCount() { return lFinalFillerColumnCount; }
    };
  }
}
