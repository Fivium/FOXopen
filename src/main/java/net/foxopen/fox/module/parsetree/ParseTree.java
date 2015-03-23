package net.foxopen.fox.module.parsetree;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.parsetree.presentationnode.ActionOutPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.BufferPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.CasePresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.CommentPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.ContainerPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.ErrorOutPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.ExprPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.ExternalURLPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.ForEachPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.GridCellPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.GridPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.GridRowPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.HeadingPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.HintOutPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.HtmlPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.IfPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.IncludeHeaderResourcesPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.IncludePresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.InfoBoxPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.MailToPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.MenuOutPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PagerControlPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.SetOutPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.TabGroupPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.TextPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.WidgetOutPresentationNode;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackFlag;
import org.apache.commons.lang3.StringUtils;

import java.io.PrintWriter;


/**
 * A tree of Presentation Nodes that hold the structure of a modules presention nodes
 */
public class ParseTree {
  private final PresentationNode mRootNode;

  /**
   * Construct the Parse Tree object starting with a presentation node DOM
   *
   * @param pRootNode starting DOM node
   */
  public ParseTree(DOM pRootNode) {
    if (pRootNode == null) {
      throw new ExInternal("Cannot generate a ParseTree from a null root node");
    }

    Track.pushInfo("ParseTree", "Constructing the parse tree");
    try {
      // Make root node by parsing the DOM node
      mRootNode = parseDOMNode(pRootNode);
      Track.info("RootNodeInfo", mRootNode.toString());
    }
    finally {
      Track.pop("ParseTree");
    }
  }

  /**
  * Take a DOM Node in and generate the generate the correct type of PresentationNode for it
  *
  * @param pNode - DOM Node to make a PresentationNode for
  */
  public static PresentationNode parseDOMNode(DOM pNode) {
    if (pNode.isElement()) {
      switch (pNode.getName()) {
        case "fm:set-buffer":
          return new BufferPresentationNode(pNode);
        case "fm:set-page":
          PresentationNode lParsedPresentationNode = new BufferPresentationNode(pNode);
          ((BufferPresentationNode)lParsedPresentationNode).setName("set-page");
          return lParsedPresentationNode;
        case "fm:include":
          return new IncludePresentationNode(pNode);
        case "fm:set-out":
          return new SetOutPresentationNode(pNode);
        case "fm:widget-out":
          return new WidgetOutPresentationNode(pNode);
        case "fm:action-out":
          return new ActionOutPresentationNode(pNode);
        case "fm:menu-out":
          return new MenuOutPresentationNode(pNode);
        case "fm:expr-out":
          return new ExprPresentationNode(pNode);
        case "fm:for-each":
          return new ForEachPresentationNode(pNode);
        case "fm:if":
          return new IfPresentationNode(pNode);
        case "fm:case":
          return new CasePresentationNode(pNode);
        case "fm:hint-out":
          return new HintOutPresentationNode(pNode);
        case "fm:error-out":
          return new ErrorOutPresentationNode(pNode);
        case "fm:external-url":
          return new ExternalURLPresentationNode(pNode);
        case "fm:mail-to":
          return new MailToPresentationNode(pNode);
        case "fm:evaluate-attributes":
          // Ignored container as all attributes are evaluated now
          return new ContainerPresentationNode(pNode);
        case "fm:heading":
          return new HeadingPresentationNode(pNode);
        case "fm:grid":
          return new GridPresentationNode(pNode);
        case "fm:row":
          return new GridRowPresentationNode(pNode);
        case "fm:cell":
          return new GridCellPresentationNode(pNode);
        case "fm:info-box":
          return new InfoBoxPresentationNode(pNode);
        case "fm:page-controls-out":
          return new PagerControlPresentationNode(pNode);
        case "fm:include-header-resources":
          return new IncludeHeaderResourcesPresentationNode(pNode);
        case "fm:tab-group":
          return new TabGroupPresentationNode(pNode);
        case "fm:tab-prompt-link":
          return new TabGroupPresentationNode.TabPromptPresentationNode(pNode);
        default:
          if (pNode.getName().startsWith("fm:")) {
            // Non-handled fm-prefixed elements throw an unhandled error
            throw new ExInternal("Unknown fm: prefixed presentation-level element: " + pNode.outputNodeToString(true));
          }
          else {
            // If it's a non-handled element that doesn't start with an fm-prefix then it's likely to be a regular HTML node
            return new HtmlPresentationNode(pNode);
          }
      }
    }
    else if (pNode.isText()) {
      return new TextPresentationNode(pNode);
    }
    else if (pNode.isComment()) {
      return new CommentPresentationNode(pNode);
    }
    else if (pNode.isProcessingInstruction()) {
      Track.info("BadMarkup", "Found a processing instruction in a buffer...this will have no effect and was probably meant to be a comment", TrackFlag.BAD_MARKUP);
    }
    else {
      throw new ExInternal("Unhandled node type: " + pNode.outputNodeToString(true));
    }

    return null;
  }

  /**
   * Recursively parse DOM Nodes to construct a PresentationNode tree from them
   *  @param pParentNode PresentationNode to add the parsed DOM node children to
   * @param pNode DOM holding un-parsed nodes
   * @param pPreserveComments
   */
  public static void parseDOMChildren(PresentationNode pParentNode, DOM pNode, boolean pPreserveComments) {
    if (pParentNode == null) {
      throw new ExInternal("Cannot parse children without a parent node to add them too");
    }

    DOMList lDOMChildNodeList = pNode.getChildNodes();
    for (DOM lDOMChildNode : lDOMChildNodeList) {
      if (!pPreserveComments && lDOMChildNode.isComment()) {
        // Don't bother parsing this child node if we're not preserving comments
        continue;
      }

      PresentationNode lChildNode = parseDOMNode(lDOMChildNode);

      if (lChildNode != null) {
        pParentNode.addChildNode(lChildNode);
      }
    }
  }

  /**
   * Get the root PresentationNode for this tree
   * @return root PresentationNode for this tree
   */
  public PresentationNode getRootNode() {
    return mRootNode;
  }

  /**
   * Print an ASCII representation of a presentation node tree starting at the root node
   *
   * @see ParseTree#printDebug(PrintWriter pWriter, int pGutter, PresentationNode pNode)
   */
  public void printDebug(PrintWriter pWriter) {
    printDebug(pWriter, 1, mRootNode);
  }

  /**
   * Print an ASCII representation of a presentation node tree starting at any node
   *
   * @see ParseTree#printDebug(PrintWriter pWriter, int pGutter, PresentationNode pNode)
   */
  public static void printDebug(PrintWriter pWriter, PresentationNode pStartNode) {
    printDebug(pWriter, 1, pStartNode);
  }

  /**
   * Print the ASCII representation of a presentation node tree
   * <pre>
   * e.g.
   * - RootNode
   * -- ChildNode1
   * --- LeafNode
   * -- ChildNode2
   * --- LeafNode1
   * --- LeafNode2
   * </pre>
   *
   * @param pGutter Size of the indent
   * @param pNode Node to start from
   */
  private static void printDebug(PrintWriter pWriter, int pGutter, PresentationNode pNode) {
    pWriter.append(StringUtils.repeat("-", pGutter) + " " + pNode.toString());
    for (PresentationNode lChild : pNode.getChildNodes()) {
      printDebug(pWriter, pGutter++, lChild);
    }
  }
}
