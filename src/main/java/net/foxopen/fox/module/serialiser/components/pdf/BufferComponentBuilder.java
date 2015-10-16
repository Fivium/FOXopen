package net.foxopen.fox.module.serialiser.components.pdf;

import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedBufferPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.pdf.pages.PageAttributes;
import net.foxopen.fox.module.serialiser.pdf.pages.PageTemplate;
import net.foxopen.fox.page.PageDefinition;

import java.util.Optional;

/**
 * Serialises a buffer
 */
public class BufferComponentBuilder
extends ComponentBuilder<PDFSerialiser, EvaluatedBufferPresentationNode> {
  private static final ComponentBuilder<PDFSerialiser, EvaluatedBufferPresentationNode> INSTANCE = new BufferComponentBuilder();

  public static final ComponentBuilder<PDFSerialiser, EvaluatedBufferPresentationNode> getInstance() {
    return INSTANCE;
  }

  private BufferComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedBufferPresentationNode pEvalBufferNode) {
    if (pEvalBufferNode.getEvalClientVisibilityRule() == null || pEvalBufferNode.getEvalClientVisibilityRule().isVisible()) {
      Optional<PageDefinition> lPageDefinition = Optional.ofNullable(pEvalBufferNode.getPageDefinition());

      lPageDefinition.ifPresent(pPageDefinition -> {
        PageTemplate lPageTemplate = new PageTemplate(PageAttributes.createFromPageDefinition(pPageDefinition));
        pSerialiser.startPageTemplate(lPageTemplate);
      });
      processChildren(pSerialisationContext, pSerialiser, pEvalBufferNode);
      lPageDefinition.ifPresent(pPageDefinition -> pSerialiser.endPageTemplate());
    }
  }
}
