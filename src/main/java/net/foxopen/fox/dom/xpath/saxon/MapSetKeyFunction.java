package net.foxopen.fox.dom.xpath.saxon;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.mapset.MapSet;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.thread.ActionRequestContext;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.option.xom.XOMNodeWrapper;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;


/**
 * Function for resolving mapset data to its corresponding key string within an XPath expression.
 */
class MapSetKeyFunction
extends ExtensionFunctionDefinition {

  MapSetKeyFunction() {}



  @Override
  public StructuredQName getFunctionQName() {
    return new StructuredQName(SaxonEnvironment.FOX_NS_PREFIX, SaxonEnvironment.FOX_NS_URI, "mapset-key");
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

        ActionRequestContext lRequestContext = SaxonEnvironment.getThreadLocalRequestContext();

        Item lItemNodeOrStringParam = pArguments[0].head();

        //First argument can be a string or node - work out which.
        DOM lItemDOM = null;
        String lItemMapSetName = null;
        String lItemMapSetAttach = null;
        String lDataString = null;
        if(lItemNodeOrStringParam instanceof StringValue) {
          lDataString = lItemNodeOrStringParam.getStringValue();
        }
        else if(lItemNodeOrStringParam instanceof XOMNodeWrapper) {
          //For nodes we can attempt to look up the mapset attribute on the NodeInfo
          lItemDOM = FunctionUtils.unwrapNode((XOMNodeWrapper) lItemNodeOrStringParam);
          if(lItemDOM != null) {
            lItemMapSetName = lRequestContext.getCurrentModule().getNodeInfoDefaultAttribute(lItemDOM, NodeAttribute.MAPSET);
            lItemMapSetAttach = lRequestContext.getCurrentModule().getNodeInfoDefaultAttribute(lItemDOM, NodeAttribute.MAPSET_ATTACH);
          }
        }
        else {
          throw new ExInternal("First argument to fox:mapset-key must be a node or string.");
        }

        //The mapset name could have been explicitly passed in as an argument
        String lMapSetNameParam = FunctionUtils.getStringValueOrNull(pArguments, 1, "fox:mapset-key");

        //Work out which mapset name to use - give precedence to the explicit argument, falling back to the value on the NodeInfo (if available)
        String lUseMapSetName = XFUtil.nvl(lMapSetNameParam, lItemMapSetName);
        if(XFUtil.isNull(lUseMapSetName)) {
          throw new ExInternal("fox:mapset-key function failed to determine mapset name - explicitly specify a name as the second argument of this function," +
                               "or ensure an element with a corresponding schema definition is supplied as the first argument.");
        }

        //Resolve the attach node if specified
        DOM lMapSetAttachDOM = FunctionUtils.getDOMNodeOrNull(pArguments, 2, "fox:mapset-key");

        //Resolve the mapset from the current module call and look up the key
        MapSet lMapSet;
        if(lMapSetAttachDOM != null) {
          //Use the attach DOM variant if an attach DOM was explicitly specified as an argument
          lMapSet = lRequestContext.resolveMapSet(lUseMapSetName, lItemDOM, lMapSetAttachDOM);
        }
        else {
          //Otherwise use the attach XPath string variant
          lMapSet = lRequestContext.resolveMapSet(lUseMapSetName, lItemDOM, lItemMapSetAttach);
        }

        String lKey;
        if(lItemDOM != null) {
          //If we have a data DOM reference use that for the mapset lookup (required for complex mapsets)
          lKey = lMapSet.getKey(lRequestContext, lItemDOM);
        }
        else {
          //If we just have a string use that
          lKey = lMapSet.getKeyForDataString(lRequestContext, lMapSetAttachDOM, lDataString);
        }

        //Wrap result and return
        return StringValue.makeStringValue(lKey);
      }
    };
  }

  public int getMinimumNumberOfArguments() {
    return 1;
  }

  public int getMaximumNumberOfArguments() {
    return 3;
  }
}
