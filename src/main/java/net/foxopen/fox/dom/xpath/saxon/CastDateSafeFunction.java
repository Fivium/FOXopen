package net.foxopen.fox.dom.xpath.saxon;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.sf.saxon.s9api.ExtensionFunction;
import net.sf.saxon.s9api.ItemType;
import net.sf.saxon.s9api.OccurrenceIndicator;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.SequenceType;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmEmptySequence;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.trans.XPathException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Attempts to cast a DOM value as a xs:date value returns an empty sequence if not a valid date
 * Usage: fox:cast-date-safe(value)
 */
public class CastDateSafeFunction
implements ExtensionFunction {

  private static final String FUNCTION_NAME = "cast-date-safe";

  CastDateSafeFunction() {
  }

  @Override
  public QName getName() {
    return new QName(SaxonEnvironment.FOX_NS_PREFIX, SaxonEnvironment.FOX_NS_URI, FUNCTION_NAME);
  }

  @Override
  public SequenceType getResultType() {
    return SequenceType.makeSequenceType(ItemType.ANY_ITEM, OccurrenceIndicator.ZERO_OR_ONE);
  }

  @Override
  public SequenceType[] getArgumentTypes() {
    return new SequenceType[]{
    SequenceType.makeSequenceType(ItemType.ANY_ITEM, OccurrenceIndicator.ZERO_OR_ONE)
    };
  }

  @Override
  public XdmValue call(XdmValue[] xdmValues)
  throws SaxonApiException {

    String lValue;

    try {
      DOM lDom = FunctionUtils.getDOMNodeOrNull(xdmValues, 0, SaxonEnvironment.FOX_NS_PREFIX + ":" + FUNCTION_NAME, false);
      if (lDom == null) {
        // if no nodes are matched return empty sequence
        return XdmEmptySequence.getInstance();
      }
      lValue = lDom.value();
    }
    catch (XPathException e) {
      throw new ExInternal("Xpath exception when attempting to parse parameters " + xdmValues[0].toString(), e);
    }

    DateFormat df = new SimpleDateFormat(XFUtil.XML_DATE_FORMAT);

    try {
      df.parse(lValue);
      return new XdmAtomicValue(lValue, ItemType.DATE);
    }
    catch (ParseException e) {
      // not in the correct format, return empty sequence
      return XdmEmptySequence.getInstance();
    }

  }

}
