package net.foxopen.fox.module.fieldset;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.EvaluatedNodeAction;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;
import net.foxopen.fox.thread.devtoolbar.DevToolbarContext;
import net.foxopen.fox.thread.ActionRequestContext;

abstract class FieldSequenceGenerator {

  static FieldSequenceGenerator createGenerator(ActionRequestContext pRequestContext, FieldSet pOwningFieldSet) {
    if(pRequestContext.getDevToolbarContext().isFlagOn(DevToolbarContext.Flag.SEMANTIC_FIELD_IDS)) {
      return new SemanticGenerator();
    }
    else {
      return new IncrementingGenerator(pOwningFieldSet);
    }
  }

  abstract String nextFieldId(EvaluatedNode pEvaluatedNode);

  static class IncrementingGenerator
  extends FieldSequenceGenerator {

    private final FieldSet mFieldSet;

    IncrementingGenerator(FieldSet pFieldSet) {
      mFieldSet = pFieldSet;
    }

    @Override
    String nextFieldId(EvaluatedNode pEvaluatedNode) {
      return "g"+(mFieldSet.getNextFieldSequence());
    }
  }

  static class SemanticGenerator
  extends FieldSequenceGenerator {

    private final Multiset<String> mIdentifierMultiset = HashMultiset.create();

    @Override
    String nextFieldId(EvaluatedNode pEvaluatedNode) {


      String lIdentifier;

      if(pEvaluatedNode instanceof EvaluatedNodeAction || pEvaluatedNode.getWidgetBuilderType() == WidgetBuilderType.BUTTON ||  pEvaluatedNode.getWidgetBuilderType() == WidgetBuilderType.LINK) {
        lIdentifier = pEvaluatedNode.getActionName();
      }
      else {
        DOM lItemDOM = pEvaluatedNode.getDataItem();
        if(lItemDOM == null) {
          lIdentifier = "unknown";
        }
        else {
          DOM lParentDOM = lItemDOM.getParentOrNull();
          //Don't use slashes as a seperator; the SingleOptionFieldMgr code relies on them (also beware: non alphanumeric chars can break jQuery selectors)
          lIdentifier = ((lParentDOM != null) ? lParentDOM.getLocalName() + "_" : "") + lItemDOM.getLocalName();
        }
      }


      int lCount = mIdentifierMultiset.count(lIdentifier);
      mIdentifierMultiset.add(lIdentifier);

      return lIdentifier + (lCount > 0  ? "_" + lCount : "");
    }
  }
}
