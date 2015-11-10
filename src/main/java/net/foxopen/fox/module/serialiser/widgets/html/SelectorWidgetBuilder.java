package net.foxopen.fox.module.serialiser.widgets.html;


import com.google.common.base.Joiner;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoItem;
import net.foxopen.fox.module.datanode.NodeVisibility;
import net.foxopen.fox.module.fieldset.fieldmgr.FieldMgr;
import net.foxopen.fox.module.fieldset.fvm.FieldSelectOption;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.fragmentbuilder.MustacheFragmentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.OptionWidgetUtils;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class SelectorWidgetBuilder extends WidgetBuilderHTMLSerialiser<EvaluatedNodeInfoItem> {
  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNodeInfoItem> INSTANCE = new SelectorWidgetBuilder();
  private static final String MUSTACHE_TEMPLATE = "html/SelectorWidget.mustache";

  public static final WidgetBuilder<HTMLSerialiser, EvaluatedNodeInfoItem> getInstance() {
    return INSTANCE;
  }

  private SelectorWidgetBuilder () {
  }

  static void outputReadOnlyOptions(HTMLSerialiser pSerialiser, EvaluatedNodeInfoItem pEvaluatedNode) {
    //Output all selected values, unless they represent a key-missing entry ("Select one" etc)
    List<String> lSelectedExternalStrings = OptionWidgetUtils.filteredReadOnlySelectorOptions(pEvaluatedNode)
      .map(FieldSelectOption::getDisplayKey)
      .collect(Collectors.toList());

    pSerialiser.append("<span class=\"text-widget\">");
    pSerialiser.append(Joiner.on("<br>").join(lSelectedExternalStrings));
    pSerialiser.append("</span>");
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNodeInfoItem pEvalNode) {
    FieldMgr lFieldMgr = pEvalNode.getFieldMgr();

    if (!pEvalNode.isPlusWidget() && lFieldMgr.getVisibility() == NodeVisibility.VIEW) {
      outputReadOnlyOptions(pSerialiser, pEvalNode);
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
