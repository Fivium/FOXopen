package net.foxopen.fox.dom.xpath.saxon;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilder;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.plugin.PluginWebServiceCategory;
import net.foxopen.fox.track.Track;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Function for generating a plugin web service URI.
 */
class PluginURIFunction
extends ExtensionFunctionDefinition {

  private static final String FUNCTION_NAME = "fox:plugin-uri";

  PluginURIFunction() {}

  @Override
  public StructuredQName getFunctionQName() {
    return new StructuredQName(SaxonEnvironment.FOX_NS_PREFIX, SaxonEnvironment.FOX_NS_URI, "plugin-uri");
  }

  @Override
  public SequenceType[] getArgumentTypes() {
    return new SequenceType[]{SequenceType.makeSequenceType(AnyItemType.getInstance(), StaticProperty.ALLOWS_ZERO_OR_ONE)};
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
        try {
          //First argument is plugin name (mandatory string)
          String lPluginName = FunctionUtils.getStringValueOrNull(pArguments, 0, FUNCTION_NAME, true);
          if(XFUtil.isNull(lPluginName)) {
            throw new ExInternal("First parameter must be a non-empty string.");
          }

          //Second argument is endpoint name (mandatory string)
          String lEndPointName = FunctionUtils.getStringValueOrNull(pArguments, 1, FUNCTION_NAME, true);
          if(XFUtil.isNull(lEndPointName)) {
            throw new ExInternal("Second parameter must be a non-empty string.");
          }

          //Construct URI param map (linked to preserve order)
          Map<String, String> lParams = new LinkedHashMap<>();
          if(pArguments.length > 2) {

            if(pArguments.length % 2 != 0) {
              throw new ExInternal("Parameter list length mismatch. " + paramDebugMessage(pArguments));
            }

            for(int i = 2; i < pArguments.length; i = i+2) {

              String lParamName = FunctionUtils.getStringValueOrNull(pArguments, i, FUNCTION_NAME, true);
              String lParamValue = FunctionUtils.getStringValueOrNull(pArguments, i + 1, FUNCTION_NAME, false);

              //Basic validation
              if(XFUtil.isNull(lParamName)) {
                throw new ExInternal("Parameter names cannot be null (index " + i + "). " + paramDebugMessage(pArguments));
              }
              else if(XFUtil.isNull(lParamValue)) {
                //Don't fail but warn
                Track.alert("PluginURIFunction", "Value for parameter " + lParamName + " was null, parameter will be excluded from URI");
              }

              lParams.put(lParamName, lParamValue);
            }
          }

          //Build URI
          RequestURIBuilder lURIBuilder = SaxonEnvironment.getThreadLocalRequestContext().createURIBuilder();
          lURIBuilder.setParams(lParams);
          String lURI = lURIBuilder.buildWebServiceURI(PluginWebServiceCategory.CATEGORY_NAME, lPluginName, lEndPointName);

          return StringValue.makeStringValue(lURI);
        }
        catch (Throwable th) {
          throw new ExInternal("Error running fox:plugin-uri function", th);
        }
      }
    };
  }

  /**
   * Gets the optional parameters passed to this function as a string of name value pairs, to help the developer debug
   * errors with the parameter list.
   * @param pArguments Argument sequence of length > 2
   * @return Parameter debug information for inclusion in exception text.
   */
  private String paramDebugMessage(Sequence[] pArguments)
  throws XPathException {
    StringBuilder lErrorMessage = new StringBuilder("Parameters evaluated as: ");
    for(int i = 2; i < pArguments.length; i++) {
      lErrorMessage.append(XFUtil.nvl(FunctionUtils.getStringValueOrNull(pArguments, i, FUNCTION_NAME, false), "[null]"));
      if(i % 2 == 0) {
        lErrorMessage.append("=");
      }
      else if(i < pArguments.length - 1) {
        lErrorMessage.append(", ");
      }
    }
    return lErrorMessage.toString();
  }

  public int getMinimumNumberOfArguments() {
    return 2;
  }

  public int getMaximumNumberOfArguments() {
    return 999;
  }
}
