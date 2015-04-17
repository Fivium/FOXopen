package net.foxopen.fox.module.serialiser.widgets.html;


import com.google.common.base.Joiner;
import net.foxopen.fox.module.fieldset.fieldmgr.FieldMgr;
import net.foxopen.fox.module.fieldset.fvm.FieldSelectOption;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.NodeVisibility;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.fragmentbuilder.MustacheFragmentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SelectorWidgetBuilder extends WidgetBuilderHTMLSerialiser<EvaluatedNode> {
  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> INSTANCE = new SelectorWidgetBuilder();
  private static final String MUSTACHE_TEMPLATE = "html/SelectorWidget.mustache";

  public static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> getInstance() {
    return INSTANCE;
  }

  private SelectorWidgetBuilder () {
  }

  static void outputReadOnlyOptions(HTMLSerialiser pSerialiser, FieldMgr pFieldMgr) {
    List<String> lSelectedExternalStrings = new ArrayList<>();
    for(FieldSelectOption lOption : pFieldMgr.getSelectOptions()) {
      //Output all selected values, unless they represent a key-missing entry ("Select one" etc)
      if(lOption.isSelected() && !lOption.isNullEntry()) {
        lSelectedExternalStrings.add(lOption.getDisplayKey());
      }
    }

    pSerialiser.append("<span class=\"text-widget\">");
    pSerialiser.append(Joiner.on("<br>").join(lSelectedExternalStrings));
    pSerialiser.append("</span>");
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNode pEvalNode) {
    FieldMgr lFieldMgr = pEvalNode.getFieldMgr();

    if (!pEvalNode.isPlusWidget() && lFieldMgr.getVisibility() == NodeVisibility.VIEW) {
      outputReadOnlyOptions(pSerialiser, lFieldMgr);
    }
    else {
      Map<String, Object> lTemplateVars = super.getGenericTemplateVars(pSerialisationContext, pSerialiser, pEvalNode);
      List<FieldSelectOption> lSelectOptions = lFieldMgr.getSelectOptions();

      if(pEvalNode.getSelectorMaxCardinality() > 1) {
        lTemplateVars.put("Multiple", true );
        lTemplateVars.put("Size", lSelectOptions.size());
      }

      List<Map<String, Object>> lOptions = new ArrayList<>();
      OPTION_LOOP:
      for (FieldSelectOption lOption : lSelectOptions) {
        if(lOption.isHistorical() && !lOption.isSelected()) {
          //Skip un-selected historical items
          continue OPTION_LOOP;
        }
        Map<String, Object> lOptionVars = new HashMap<>(3);
        lOptionVars.put("Key", lOption.getDisplayKey());
        lOptionVars.put("Value", lOption.getExternalFieldValue());
        if (lOption.isSelected()) {
          lOptionVars.put("Selected", true);
        }
        if (lOption.isDisabled()) {
          lOptionVars.put("Disabled", true);
        }
        lOptions.add(lOptionVars);
      }
      lTemplateVars.put("Options", lOptions);

      MustacheFragmentBuilder.applyMapToTemplate(MUSTACHE_TEMPLATE, lTemplateVars, pSerialiser.getWriter());
    }
  }
}
