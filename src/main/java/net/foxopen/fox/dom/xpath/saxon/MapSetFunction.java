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
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.value.SequenceType;


/**
 * Function for retrieving a mapset DOM (i.e. a container of "rec" elements) within an XPath expression.
 */
class MapSetFunction
extends ExtensionFunctionDefinition {

  private static final String FUNCTION_NAME = "fox:mapset";

  MapSetFunction() {}

  @Override
  public StructuredQName getFunctionQName() {
    return new StructuredQName(SaxonEnvironment.FOX_NS_PREFIX, SaxonEnvironment.FOX_NS_URI, "mapset");
  }

  @Override
  public SequenceType[] getArgumentTypes() {
    return new SequenceType[]{SequenceType.makeSequenceType(AnyItemType.getInstance(), StaticProperty.ALLOWS_ZERO_OR_ONE)};
  }

  @Override
  public SequenceType getResultType(SequenceType[] pSequenceTypes) {
    return SequenceType.NODE_SEQUENCE;
  }

  @Override
  public ExtensionFunctionCall makeCallExpression() {
    return new ExtensionFunctionCall() {
      @Override
      public Sequence call(XPathContext pXPathContext, Sequence[] pArguments)
      throws XPathException {

        ActionRequestContext lRequestContext = SaxonEnvironment.getThreadLocalRequestContext();

        //First argument is mapset name (mandatory string)
        String lMapSetName = FunctionUtils.getStringValueOrNull(pArguments, 0, FUNCTION_NAME);
        if(XFUtil.isNull(lMapSetName)) {
          throw new ExInternal("First parameter to fox:mapset function must be a non-empty string.");
        }

        //Optional arguments
        DOM lMapSetItemDOM = FunctionUtils.getDOMNodeOrNull(pArguments, 1, FUNCTION_NAME);
        DOM lMapSetAttachDOM = FunctionUtils.getDOMNodeOrNull(pArguments, 2, FUNCTION_NAME);

        //Look up the attach attribute from the module schema if an explicit attach point is not provided to the function (and an item is)
        String lMapSetAttachXPath = null;
        if(lMapSetAttachDOM == null && lMapSetItemDOM != null) {
          lMapSetAttachXPath = lRequestContext.getCurrentModule().getNodeInfoDefaultAttribute(lMapSetItemDOM, NodeAttribute.MAPSET_ATTACH);
        }

        //Look up the mapset using either the explicitly specified attach DOM or the implicit attach XPath
        MapSet lMapSet;
        if(lMapSetAttachDOM != null) {
          lMapSet = lRequestContext.resolveMapSet(lMapSetName, lMapSetItemDOM, lMapSetAttachDOM);
        }
        else {
          lMapSet = lRequestContext.resolveMapSet(lMapSetName, lMapSetItemDOM, lMapSetAttachXPath);
        }

        return lMapSet.getMapSetAsDOM().get1EOrNull("*").wrap();
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
