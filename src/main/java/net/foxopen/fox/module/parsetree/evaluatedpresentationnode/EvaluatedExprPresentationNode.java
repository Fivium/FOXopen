package net.foxopen.fox.module.parsetree.evaluatedpresentationnode;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.xpath.XPathResult;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.mapset.MapSet;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.presentationnode.ExprPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;
import net.foxopen.fox.util.StringFormatter;

/**
 * Evaluate an fm:expr-out and store the resulting simple text for serialisation
 */
public class EvaluatedExprPresentationNode extends EvaluatedPresentationNode<PresentationNode> {
  private final String mEvaluatedExpression;
  private final boolean mEscapingRequired;

  /**
   * Evaluate a ExprPresentationNode object by evaluating the match attribute and applying a format mask, mapset or type conversion
   * if needed based on the type, formatMask or mapset attributes on fm:expr-out
   *
   * @param pParent
   * @param pOriginalPresentationNode
   * @param pEvaluatedParseTree
   * @param pEvalContext
   */
  public EvaluatedExprPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParent, ExprPresentationNode pOriginalPresentationNode, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    super(pParent, pOriginalPresentationNode, pEvalContext);

    String lMatchExpr = pOriginalPresentationNode.getMatch();
    String lType = pOriginalPresentationNode.getType();
    String lMask = pOriginalPresentationNode.getFormatMask();

    // Must have a match
    if (XFUtil.isNull(lMatchExpr)) {
      mEvaluatedExpression = null;
      mEscapingRequired = false;
    }
    else {
      try {
        XPathResult lXPathResult = pEvaluatedParseTree.getContextUElem().extendedXPathResult(getEvalContext(), lMatchExpr);
        String lDisplayString = lXPathResult.asString();

        // String or format not specified, return as-is
        if (XFUtil.isNull(lType) || "xs:string".equals(lType)) {
          //Look up a mapset key if a mapset name is specified
          String lMapSetName = pOriginalPresentationNode.getMapSetName();
          if(!XFUtil.isNull(lMapSetName)) {
            mEvaluatedExpression = evalMapSet(lMapSetName, lXPathResult, pOriginalPresentationNode, pEvaluatedParseTree);
          }
          else {
            mEvaluatedExpression = lDisplayString;
          }
          mEscapingRequired = true;
        }
        else if ("xs:date".equals(lType)) {
          // Convert to date using mask
          mEvaluatedExpression = StringFormatter.formatDateString (
            lDisplayString                                            // input string
            , StringFormatter.XML_DATE_FORMAT_MASK                    // input format
            , XFUtil.nvl(lMask, StringFormatter.ORA_DATE_FORMAT_MASK) // output format
          );
          mEscapingRequired = true;
        }
        else if ("xs:dateTime".equals(lType)) {
          // Convert to datetime using mask
          mEvaluatedExpression = StringFormatter.formatDateString (
            lDisplayString                                                // input string
            , StringFormatter.XML_DATETIME_FORMAT_MASK                    // input format
            , XFUtil.nvl(lMask, StringFormatter.ORA_DATETIME_FORMAT_MASK) // output format
          );
          mEscapingRequired = true;
        }
        else if ("xs:decimal".equals(lType) && !XFUtil.isNull(lMask)) {
          // Convert to decimal format using mask
          mEvaluatedExpression = StringFormatter.formatDecimalString (
            lDisplayString      // input string
          , lMask               // output format
          );
          mEscapingRequired = true;
        }
        else if ("xs:anyType".equals(lType)) {
          // Output raw HTML
          DOM lExprDOM = lXPathResult.asResultDOMOrNull();
          if (lExprDOM != null) {
            mEvaluatedExpression = lExprDOM.outputNodeToString(false);
            mEscapingRequired = false; // Don't escape raw html
          }
          else {
            mEvaluatedExpression = "";
            mEscapingRequired = true;
          }
        }
        // Just return the string as-is
        else {
          mEvaluatedExpression = lDisplayString;
          mEscapingRequired = true;
        }
      }
      catch (Throwable e) {
        throw new ExInternal("exprOut failed: " + this.toString(),e);
      }
    }
  }

  private String evalMapSet(String pMapSetName, XPathResult pXPathResult, ExprPresentationNode pOriginalPresentationNode, EvaluatedParseTree pEvaluatedParseTree)
  throws ExActionFailed, ExCardinality {

    String lMSItemXPath = pOriginalPresentationNode.getMapSetItemXPath();

    //Attempt to get a DOM node reference from the XPath expression
    DOM lExprOutTargetDOM = pXPathResult.asResultDOMOrNull();

    DOM lMSItemDOM;
    if(XFUtil.isNull(lMSItemXPath)) {
      //If not MS item is specified, use the node resolved by the XPath (if available)
      lMSItemDOM = lExprOutTargetDOM;
    }
    else {
      //If a path for the MS item was specified, resolve the node
      lMSItemDOM = pEvaluatedParseTree.getContextUElem().extendedXPath1E(getEvalContext(), lMSItemXPath);
    }

    //Resolve the mapset
    MapSet lMapSet = pEvaluatedParseTree.resolveMapSet(pMapSetName, lMSItemDOM, pOriginalPresentationNode.getMapSetAttachXPath());

    if(lExprOutTargetDOM != null) {
      //If we have a DOM reference we can use the DOM for a mapset key lookup
      return lMapSet.getKey(pEvaluatedParseTree.getRequestContext(), lExprOutTargetDOM);
    }
    else {
      //If we only have a string use the string lookup method (won't work for complex mapsets)
      return lMapSet.getKeyForDataString(pEvaluatedParseTree.getRequestContext(), lMSItemDOM, pXPathResult.asString());
    }
  }

  @Override
  public String getText() {
    return mEvaluatedExpression;
  }

  @Override
  public boolean isEscapingRequired() {
    return mEscapingRequired;
  }

  @Override
  public String toString() {
    return getOriginalNode().toString() + " = " + mEvaluatedExpression;
  }

  @Override
  public ComponentBuilderType getPageComponentType() {
    return ComponentBuilderType.EXPR_OUT;
  }
}
