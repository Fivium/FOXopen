package net.foxopen.fox.dom.xpath.saxon;


import net.sf.saxon.s9api.ExtensionFunction;
import net.sf.saxon.s9api.ItemType;
import net.sf.saxon.s9api.OccurrenceIndicator;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SequenceType;
import net.sf.saxon.s9api.XdmValue;


/**
 * Implementation of the Fox nvl() function. This mimics its Oracle namesake by returning the second argument if the
 * first is equivalent to null.
 */
public class NvlFunction
implements ExtensionFunction  {

  /**
   * Only SaxonEnvironment may instantiate this object.
   */
  NvlFunction (){}

  public QName getName() {
    return new QName(SaxonEnvironment.FOX_NS_PREFIX, SaxonEnvironment.FOX_NS_URI, "nvl");
  }

  public SequenceType getResultType() {
    return SequenceType.makeSequenceType(ItemType.ANY_ITEM, OccurrenceIndicator.ZERO_OR_MORE);
  }

  public SequenceType[] getArgumentTypes() {
    return new SequenceType[]{
      SequenceType.makeSequenceType(ItemType.ANY_ITEM, OccurrenceIndicator.ZERO_OR_ONE),
      SequenceType.makeSequenceType(ItemType.ANY_ITEM, OccurrenceIndicator.ZERO_OR_ONE)
    };
  }

  public XdmValue call(XdmValue[] xdmValues) {
    return SaxonEnvironment.isValueNull(xdmValues[0]) ? xdmValues[1] : xdmValues[0];
  }
}