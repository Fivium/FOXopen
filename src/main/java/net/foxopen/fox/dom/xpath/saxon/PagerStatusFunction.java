package net.foxopen.fox.dom.xpath.saxon;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.paging.Pager;
import net.foxopen.fox.dom.paging.PagerProvider;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.thread.ActionRequestContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.IntegerValue;
import net.sf.saxon.value.SequenceType;


/**
 * Function for attributes of a pager (i.e. for paginated query results).
 */
class PagerStatusFunction
extends ExtensionFunctionDefinition {

  private static final String FUNCTION_NAME = "fox:pager-status";

  PagerStatusFunction() {}

  @Override
  public StructuredQName getFunctionQName() {
    return new StructuredQName(SaxonEnvironment.FOX_NS_PREFIX, SaxonEnvironment.FOX_NS_URI, "pager-status");
  }

  @Override
  public SequenceType[] getArgumentTypes() {
    return new SequenceType[] {
      SequenceType.SINGLE_STRING,
      SequenceType.SINGLE_STRING,
      SequenceType.OPTIONAL_NODE
    };
  }

  @Override
  public SequenceType getResultType(SequenceType[] pSequenceTypes) {
    return SequenceType.OPTIONAL_INTEGER;
  }

  @Override
  public ExtensionFunctionCall makeCallExpression() {
    return new ExtensionFunctionCall() {
      @Override
      public Sequence call(XPathContext pXPathContext, Sequence[] pArguments)
      throws XPathException {

        ActionRequestContext lRequestContext = SaxonEnvironment.getThreadLocalRequestContext();

        //First argument is status type (mandatory string)
        String lStatusType = FunctionUtils.getStringValueOrNull(pArguments, 0, FUNCTION_NAME);
        if(XFUtil.isNull(lStatusType)) {
          throw new ExInternal("First parameter to fox:pager-status function must be a non-empty string.");
        }

        //Second argument is invoke name (mandatory string)
        String lInvokeName = FunctionUtils.getStringValueOrNull(pArguments, 1, FUNCTION_NAME);
        if(XFUtil.isNull(lInvokeName)) {
          throw new ExInternal("Second parameter to fox:pager-status function must be a non-empty string.");
        }

        //Optional 3rd argument for getting match id
        DOM lMatchDOM = FunctionUtils.getDOMNodeOrNull(pArguments, 2, FUNCTION_NAME);
        String lMatchFoxId = null;
        if(lMatchDOM != null) {
          lMatchFoxId = lMatchDOM.getFoxId();
        }

        Pager lPager = lRequestContext.getModuleFacetProvider(PagerProvider.class).getPagerOrNull(lInvokeName, lMatchFoxId);
        if(lPager != null) {
          Item lResult;
          switch(lStatusType) {
            case "current-page":
              lResult = IntegerValue.makeIntegerValue((double) lPager.getCurrentPage()).asAtomic();
              break;
            case "page-count":
              lResult = IntegerValue.makeIntegerValue((double) lPager.getPageCount()).asAtomic();
              break;
            case "page-size":
              lResult = IntegerValue.makeIntegerValue((double) lPager.getPageSize()).asAtomic();
              break;
            case "row-count":
              lResult = IntegerValue.makeIntegerValue((double) lPager.getRowCount()).asAtomic();
              break;
            default:
              throw new ExInternal("Unrecognised value for status type parameter: " + lStatusType);
          }

          return lResult;
        }
        else {
          return EmptySequence.getInstance();
        }
      }
    };
  }

  public int getMinimumNumberOfArguments() {
    return 2;
  }

  public int getMaximumNumberOfArguments() {
    return 3;
  }
}
