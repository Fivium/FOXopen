package net.foxopen.fox.module.parsetree;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.facet.ModalPopover;
import net.foxopen.fox.module.facet.ModalPopoverProvider;
import net.foxopen.fox.module.fieldset.InternalHiddenField;
import net.foxopen.fox.module.fieldset.ModalPopoverHiddenField;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.serialiser.OutputSerialiser;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.track.Track;

/**
 * Object containing information a serialiser needs in order to render a modal popover on the screen, including the evaluated
 * content buffer. This is effectively equivelant to an EvaluatedPresentationNode but does not extend that class because it
 * is not part of the parse tree hierarchy.
 */
public class EvaluatedModalPopover {

  /** The ModalPopover facet to be displayed */
  private final ModalPopover mModalPopover;

  /** Evaluated buffer containing the modal's contents */
  private final EvaluatedPresentationNode<? extends PresentationNode> mBuffer;

  /** Hidden field for preserving modal scroll position */
  private final ModalPopoverHiddenField mHiddenField;

  /**
   * Creates a new EvaluatedModalPopover if a modal popover is currently active, or returns null if no popver is active.
   * @param pRequestContext Current RequestContext.
   * @param pParseTree Current ParseTree being evaluated.
   * @return New EvaluatedModalPopover or null.
   */
  static EvaluatedModalPopover getEvaluatedPopoverOrNull(ActionRequestContext pRequestContext, EvaluatedParseTree pParseTree) {

    ModalPopover lModalPopover = pRequestContext.getModuleFacetProvider(ModalPopoverProvider.class).getCurrentPopoverOrNull();
    if(lModalPopover != null) {
      Track.pushInfo("EvaluateModalPopover");
      try {
        //Locate the modal buffer attach point
        DOM lBufferAttach = pRequestContext.getContextUElem().getElemByRef(lModalPopover.getBufferAttachFoxId());

        //Evaluate the modal's implicated buffer
        EvaluatedPresentationNode<? extends PresentationNode> lModalPopoverBuffer = pParseTree.evaluateNode(null, pParseTree.getBuffer(lModalPopover.getBufferName()), lBufferAttach);

        //Construct and register the hidden field for tracking the modal's scroll position
        ModalPopoverHiddenField lHiddenField = new ModalPopoverHiddenField(Integer.toString(lModalPopover.getScrollPosition()));
        lHiddenField.addToFieldSet(pParseTree.getFieldSet());

        return new EvaluatedModalPopover(lModalPopover, lModalPopoverBuffer, lHiddenField);
      }
      catch (Throwable th) {
        throw new ExInternal("Failed to evaluate modal popover", th);
      }
      finally {
        Track.pop("EvaluateModalPopover");
      }
    }
    else {
      return null;
    }
  }

  private EvaluatedModalPopover(ModalPopover pModalPopover, EvaluatedPresentationNode<? extends PresentationNode> pBuffer, ModalPopoverHiddenField pHiddenField) {
    mModalPopover = pModalPopover;
    mBuffer = pBuffer;
    mHiddenField = pHiddenField;
  }

  /**
   * Renders the contents of this modal popover's content buffer to the given serialiser.
   * @param pSerialisationContext Current SerialisationContext.
   * @param pSerialiser Destination for the buffer render result.
   */
  public void render(SerialisationContext pSerialisationContext, OutputSerialiser pSerialiser) {
    ComponentBuilder lBuilder = pSerialiser.getComponentBuilder(mBuffer.getPageComponentType());
    lBuilder.buildComponent(pSerialisationContext, pSerialiser, mBuffer);
  }

  /**
   * @return Gets the facet information for the modal popover currently being displayed.
   */
  public ModalPopover getModalPopover() {
    return mModalPopover;
  }

  /**
   * @return Gets the hidden field to be used to preserve the scroll position of this modal popover.
   */
  public InternalHiddenField getScrollPositionHiddenField() {
    return mHiddenField;
  }
}
