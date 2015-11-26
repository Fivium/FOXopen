package net.foxopen.fox.dom.xpath.saxon;

import net.sf.saxon.s9api.ExtensionFunction;
import net.sf.saxon.s9api.ItemType;
import net.sf.saxon.s9api.OccurrenceIndicator;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SequenceType;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.value.StringValue;


/**
 * Implementation of Fox exists-context() function.
 */
public class ExistsContextFunction
implements ExtensionFunction  {

  /**
   * Only SaxonEnvironment may instantiate this object.
   */
  ExistsContextFunction (){}

  private boolean evaluateInternal(String pLabel){
    return SaxonEnvironment.getThreadLocalContextUElem().existsContext(pLabel);
  }

  public QName getName() {
    return new QName(SaxonEnvironment.FOX_NS_PREFIX, SaxonEnvironment.FOX_NS_URI, "exists-context");
  }

  public SequenceType getResultType() {
    return SequenceType.makeSequenceType(ItemType.BOOLEAN, OccurrenceIndicator.ONE);
  }

  public SequenceType[] getArgumentTypes() {
    return new SequenceType[]{SequenceType.makeSequenceType(ItemType.STRING, OccurrenceIndicator.ONE)};
  }

  public XdmValue call(XdmValue[] xdmValues) {
    String lLabel = ((StringValue) xdmValues[0].getUnderlyingValue()).getStringValue();
    return new XdmAtomicValue(evaluateInternal(lLabel));
  }
}
