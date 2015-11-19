package net.foxopen.fox.dom.xpath.saxon;

import com.google.common.io.BaseEncoding;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.ex.ExInternal;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;

import java.io.UnsupportedEncodingException;


/**
 * Exposes a base32 encoder for application developers to use.
 */
public class EncodeBase32Function
extends ExtensionFunctionDefinition {

  public static final String FUNCTION_NAME = "encode-base32";

  /**
   * Only SaxonEnvironment may instantiate this object.
   */
  EncodeBase32Function(){}

  @Override
  public StructuredQName getFunctionQName() {
    return new StructuredQName(SaxonEnvironment.FOX_NS_PREFIX, SaxonEnvironment.FOX_NS_URI, FUNCTION_NAME);
  }

  @Override
  public SequenceType[] getArgumentTypes() {
    return new SequenceType[]{
      SequenceType.SINGLE_STRING,
      SequenceType.OPTIONAL_ITEM,
      SequenceType.OPTIONAL_INTEGER,
      SequenceType.OPTIONAL_STRING,
    };
  }

  @Override
  public int getMaximumNumberOfArguments() {
    return 4;
  }

  @Override
  public int getMinimumNumberOfArguments() {
    return 1;
  }

  @Override
  public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
    return SequenceType.SINGLE_STRING;
  }

  @Override
  public ExtensionFunctionCall makeCallExpression() {
    return new ExtensionFunctionCall() {
      @Override
      public Sequence call(XPathContext context, Sequence[] pArguments)
      throws XPathException {

        try {
          String lValueToEncode = FunctionUtils.getStringValueOrNull(pArguments, 0, FUNCTION_NAME);
          if(XFUtil.isNull(lValueToEncode)) {
            throw new ExInternal("Value to encode cannot be null or empty");
          }

          //Padding is on by default
          boolean lPad = true;
          if(pArguments.length > 1) {
            lPad = FunctionUtils.getBooleanValue(pArguments, 1);
          }

          //Separator gap size and character sequence
          int lSeparateEvery = 0;
          if(pArguments.length > 2) {
            Integer lIntValue = FunctionUtils.getIntegerValueOrNull(pArguments, 2);
            lSeparateEvery = lIntValue == null ? 0 : lIntValue;
          }

          String lSeparator = " ";
          if(pArguments.length > 3) {
            lSeparator = XFUtil.nvl(FunctionUtils.getStringValueOrNull(pArguments, 3, FUNCTION_NAME, false));
          }

          //Set up Guava encoder
          BaseEncoding lEncoder = BaseEncoding.base32();

          if (!lPad) {
            lEncoder = lEncoder.omitPadding();
          }

          if (lSeparateEvery > 0) {
            lEncoder = lEncoder.withSeparator(lSeparator, lSeparateEvery);
          }

          //Encode UTF-8 string bytes
          String lResult;
          try {
            lResult = lEncoder.encode(lValueToEncode.getBytes("UTF-8"));
          }
          catch (UnsupportedEncodingException e) {
            throw new ExInternal("Failed to get bytes to encode", e);
          }

          return StringValue.makeStringValue(lResult);
        }
        catch (Throwable th) {
          throw new ExInternal("Error executing encode-base32 function", th);
        }
      }
    };
  }
}
