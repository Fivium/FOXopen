package net.foxopen.fox.module.parsetree;

import java.io.PrintWriter;

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
import net.foxopen.fox.module.parsetree.presentationnode.TabGroupPresentationNode.TabPromptPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.TextPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.WidgetOutPresentationNode;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackFlag;

import org.apache.commons.lang3.StringUtils;


/**
 * A tree of Presentation Nodes that hold the structure of a modules presention nodes
 */
public class ParseTree {
  // TODO - NP - These constants are usually set elsewhere, backfit this later
  private static final boolean PRESERVE_WHITESPACE = true;
  private static final boolean PRESERVE_COMMENTS = true;

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
    PresentationNode lParsedPresentationNode = null;
    String lNodeName = pNode.getName().intern();

    //TODO PN/NP - switch on string
    if (lNodeName == "fm:set-buffer") {
      lParsedPresentationNode = new BufferPresentationNode(pNode);
    }
    else if (lNodeName == "fm:set-page") {
      lParsedPresentationNode = new BufferPresentationNode(pNode);
      ((BufferPresentationNode)lParsedPresentationNode).setName("set-page");
    }
    else if (lNodeName == "fm:include") {
      lParsedPresentationNode = new IncludePresentationNode(pNode);
    }
    else if (lNodeName == "fm:set-out") {
      lParsedPresentationNode = new SetOutPresentationNode(pNode);
    }
    else if (lNodeName == "fm:widget-out") {
      lParsedPresentationNode = new WidgetOutPresentationNode(pNode);
    }
    else if (lNodeName == "fm:action-out") {
      lParsedPresentationNode = new ActionOutPresentationNode(pNode);
    }
    else if (lNodeName == "fm:menu-out") {
      lParsedPresentationNode = new MenuOutPresentationNode(pNode);
    }
    else if (lNodeName == "fm:for-each") {
      lParsedPresentationNode = new ForEachPresentationNode(pNode);
    }
    else if (lNodeName == "fm:expr-out") {
      lParsedPresentationNode = new ExprPresentationNode(pNode);
    }
    else if (lNodeName == "fm:if") {
      lParsedPresentationNode = new IfPresentationNode(pNode);
    }
    else if (lNodeName == "fm:case") {
      lParsedPresentationNode = new CasePresentationNode(pNode);
    }
    else if (lNodeName == "fm:hint-out") {
      lParsedPresentationNode = new HintOutPresentationNode(pNode);
    }
    else if (lNodeName == "fm:error-out") {
      lParsedPresentationNode = new ErrorOutPresentationNode(pNode);
    }
    else if (lNodeName == "fm:external-url") {
      lParsedPresentationNode = new ExternalURLPresentationNode(pNode);
    }
    else if (lNodeName == "fm:mail-to") {
      lParsedPresentationNode = new MailToPresentationNode(pNode);
    }
    else if (lNodeName == "fm:evaluate-attributes") {
      lParsedPresentationNode = new ContainerPresentationNode(pNode); // Ignored container as all attributes are evaluated now
    }
    else if (lNodeName == "fm:heading") {
      lParsedPresentationNode = new HeadingPresentationNode(pNode);
    }
    else if (lNodeName == "fm:grid") {
      lParsedPresentationNode = new GridPresentationNode(pNode);
    }
    else if (lNodeName == "fm:row") {
      lParsedPresentationNode = new GridRowPresentationNode(pNode);
    }
    else if (lNodeName == "fm:cell") {
      lParsedPresentationNode = new GridCellPresentationNode(pNode);
    }
    else if (lNodeName == "fm:info-box") {
      lParsedPresentationNode = new InfoBoxPresentationNode(pNode);
    }
    else if (lNodeName == "fm:page-controls-out") {
      lParsedPresentationNode = new PagerControlPresentationNode(pNode);
    }
    else if (lNodeName == "fm:include-header-resources") {
      lParsedPresentationNode = new IncludeHeaderResourcesPresentationNode(pNode);
    }
    else if (lNodeName == "fm:tab-group") {
      lParsedPresentationNode = new TabGroupPresentationNode(pNode);
    }
    else if (lNodeName == "fm:tab-prompt-link") {
      lParsedPresentationNode = new TabPromptPresentationNode(pNode);
    }
    else if (pNode.isElement()) {
      if (lNodeName.startsWith("fm:")) {
        // Non-handled fm-prefixed elements throw an unhandled error
        throw new ExInternal("Unknown fm: prefixed presentation-level element: " + pNode.outputNodeToString(true));
      }

      lParsedPresentationNode = new HtmlPresentationNode(pNode);
    }
    else if (pNode.isText()) {
      lParsedPresentationNode = new TextPresentationNode(pNode);
      if (!PRESERVE_WHITESPACE && ((TextPresentationNode)lParsedPresentationNode).getText().trim().length() == 0) {
        lParsedPresentationNode = null;
      }
    }
    else if (pNode.isComment()) {
      if (!PRESERVE_COMMENTS) {
        lParsedPresentationNode = null;
      }
      else {
        lParsedPresentationNode = new CommentPresentationNode(pNode);
      }
    }
    else if (pNode.isProcessingInstruction()) {
      Track.info("BadMarkup", "Found a processing instruction in a buffer...this will have no effect and was probably meant to be a comment", TrackFlag.BAD_MARKUP);
    }
    else {
      throw new ExInternal("Unhandled node type: " + pNode.outputNodeToString(true));
    }

    return lParsedPresentationNode;
  }

  /**
   * Recursively parse DOM Nodes to construct a PresentationNode tree from them
   *
   * @param pParentNode PresentationNode to add the parsed DOM noce children to
   * @param pNode DOM holding un-parsed nodes
   */
  public static void parseDOMChildren(PresentationNode pParentNode, DOM pNode) {
    if (pParentNode == null) {
      throw new ExInternal("Cannot parse children without a parent node to add them too");
    }

    DOMList lDOMChildNodeList = pNode.getChildNodes();
    int lDOMChildCount = lDOMChildNodeList.getLength();
    for (int i = 0; i < lDOMChildCount; i++) {
      PresentationNode lChildNode = null;

      lChildNode = parseDOMNode(lDOMChildNodeList.item(i));

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
