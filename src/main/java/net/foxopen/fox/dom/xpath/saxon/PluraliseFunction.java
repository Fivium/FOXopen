package net.foxopen.fox.dom.xpath.saxon;

import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.util.StringPluraliser;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;


/**
 * Function for pluralising a word based on a count, and prepending the count to the word.
 */
class PluraliseFunction
extends ExtensionFunctionDefinition {

  private static final String FUNCTION_NAME = "fox:pluralise";

  PluraliseFunction() {}

  @Override
  public StructuredQName getFunctionQName() {
    return new StructuredQName(SaxonEnvironment.FOX_NS_PREFIX, SaxonEnvironment.FOX_NS_URI, "pluralise");
  }

  @Override
  public SequenceType[] getArgumentTypes() {
    return new SequenceType[]{SequenceType.SINGLE_NUMERIC, SequenceType.OPTIONAL_STRING};
  }

  @Override
  public SequenceType getResultType(SequenceType[] pSequenceTypes) {
    return SequenceType.SINGLE_STRING;
  }

  @Override
  public ExtensionFunctionCall makeCallExpression() {
    return new ExtensionFunctionCall() {
      @Override
      public Sequence call(XPathContext pXPathContext, Sequence[] pArguments)
      throws XPathException {

        //First argument is mandatory integer representing the count
        Integer lCount = FunctionUtils.getIntegerValueOrNull(pArguments, 0);
        if (lCount == null) {
          throw new ExInternal("A count must be provided to fm:pluralise");
        }

        String lResult;
        if (pArguments.length == 2) {
          //Simple pluralise - just append an 's' for plurals
          lResult = StringPluraliser.pluralise(lCount, FunctionUtils.getStringValueOrNull(pArguments, 1, FUNCTION_NAME, false));
        }
        else {
          //Explicit pluralise - choose the form from 2 options, based on count
          String lSingleForm = FunctionUtils.getStringValueOrNull(pArguments, 1, FUNCTION_NAME, false);
          String lPluralForm = FunctionUtils.getStringValueOrNull(pArguments, 2, FUNCTION_NAME, false);
          lResult = StringPluraliser.explicitPluralise(lCount, lSingleForm, lPluralForm);
        }

        return new StringValue(lResult);
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
