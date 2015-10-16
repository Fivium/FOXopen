package net.foxopen.fox.module.serialiser.components.pdf;

import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;

import java.util.Optional;

/**
 * Serialises text. All leading and trailing whitespace is removed, and any sequence of whitespace is within the text is
 * replaced with a single space.
 */
public class TextComponentBuilder extends ComponentBuilder<PDFSerialiser, EvaluatedPresentationNode> {
  public static final String ANY_WHITESPACE_BUT_NBSP_REGEX = "[^\\S\\xA0]+";
  private static final ComponentBuilder<PDFSerialiser, EvaluatedPresentationNode> INSTANCE = new TextComponentBuilder();

  public static final ComponentBuilder<PDFSerialiser, EvaluatedPresentationNode> getInstance() {
    return INSTANCE;
  }

  private TextComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedPresentationNode pEvalNode) {
    Optional.ofNullable(pEvalNode.getText()).ifPresent(pText -> {
      // To replicate how HTML renders whitespace, remove all leading and trailing whitespace apart from any
      // non-breaking spaces, and replace any sequence of whitespace except non-breaking space within the text with a
      // single space.
      String lTrimmedText = pText.replaceAll("^" + ANY_WHITESPACE_BUT_NBSP_REGEX, "")
                                 .replaceAll(ANY_WHITESPACE_BUT_NBSP_REGEX + "$", "")
                                 .replaceAll(ANY_WHITESPACE_BUT_NBSP_REGEX, " ");

      // Add the text if it wasn't only whitespace
      if (!"".equals(lTrimmedText)) {
        pSerialiser.addText(lTrimmedText);
      }
    });
  }
}
