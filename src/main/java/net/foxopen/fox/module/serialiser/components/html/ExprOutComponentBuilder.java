package net.foxopen.fox.module.serialiser.components.html;

import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import org.apache.commons.lang3.StringEscapeUtils;

/**
 * Similar to the TextComponentBuilder, but linebreaks are converted for the serialiser
 */
public class ExprOutComponentBuilder extends ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> {
  private static final ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> INSTANCE = new ExprOutComponentBuilder();

  public static final ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> getInstance() {
    return INSTANCE;
  }

  private ExprOutComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedPresentationNode pEvalNode) {
    String lEvaluatedExpression = pEvalNode.getText();
    if (pEvalNode.isEscapingRequired()) {
      lEvaluatedExpression = StringEscapeUtils.escapeHtml4(lEvaluatedExpression);
    }
    pSerialiser.append(pSerialiser.escapeNewlines(lEvaluatedExpression));
  }
}
