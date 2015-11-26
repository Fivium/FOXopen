package net.foxopen.fox.dom.xpath.saxon;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.s9api.ExtensionFunction;
import net.sf.saxon.s9api.ItemType;
import net.sf.saxon.s9api.OccurrenceIndicator;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SequenceType;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.value.StringValue;


/**
 * Implementation of fox:ctxt() (aka :{context}) function.
 */
public class FoxContextFunction
implements ExtensionFunction {

  private NodeInfo evaluateInternal(String pLabel){
    ContextUElem lContextUElem = SaxonEnvironment.getThreadLocalContextUElem();
    DOM lDOM = lContextUElem.getUElem(pLabel);

    if(!lDOM.isAttached()) {
      throw new ExInternal("Node with context label " + pLabel + " is unattached and cannot be used for XPath evaluation");
    }

    return lDOM.wrap();
  }

  public QName getName() {
    return new QName(SaxonEnvironment.FOX_NS_PREFIX, SaxonEnvironment.FOX_NS_URI, "ctxt");
  }

  public SequenceType getResultType() {
    return SequenceType.makeSequenceType(ItemType.ANY_NODE, OccurrenceIndicator.ONE);
  }

  public SequenceType[] getArgumentTypes() {
    return new SequenceType[]{SequenceType.makeSequenceType(ItemType.STRING, OccurrenceIndicator.ONE)};
  }

  public XdmValue call(XdmValue[] xdmValues) {
    String lLabel = ((StringValue) xdmValues[0].getUnderlyingValue()).getStringValue();

    if(XFUtil.isNull(lLabel)) {
      throw new ExInternal("Empty context label specified in call to fox:ctxt function");
    }

    return new XdmNode(evaluateInternal(lLabel));
  }
}
