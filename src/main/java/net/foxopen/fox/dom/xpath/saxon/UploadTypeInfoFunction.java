package net.foxopen.fox.dom.xpath.saxon;


import net.foxopen.fox.FileUploadType;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.datanode.NodeInfo;
import net.foxopen.fox.thread.ActionRequestContext;
import net.sf.saxon.s9api.ExtensionFunction;
import net.sf.saxon.s9api.ItemType;
import net.sf.saxon.s9api.OccurrenceIndicator;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SequenceType;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmEmptySequence;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.trans.XPathException;

import java.util.ArrayList;
import java.util.List;


/**
 * Function for displaying information about a file upload type, including its restrictions.
 */
public class UploadTypeInfoFunction
implements ExtensionFunction  {

  /**
   * Only SaxonEnvironment may instantiate this object.
   */
  UploadTypeInfoFunction (){}

  public QName getName() {
    return new QName(SaxonEnvironment.FOX_NS_PREFIX, SaxonEnvironment.FOX_NS_URI, "upload-type-info");
  }

  public SequenceType getResultType() {
    return SequenceType.makeSequenceType(ItemType.STRING, OccurrenceIndicator.ZERO_OR_MORE);
  }

  public SequenceType[] getArgumentTypes() {
    return new SequenceType[]{
      SequenceType.makeSequenceType(ItemType.ANY_NODE, OccurrenceIndicator.ZERO_OR_ONE),
      SequenceType.makeSequenceType(ItemType.STRING, OccurrenceIndicator.ONE)
    };
  }

  public XdmValue call(XdmValue[] pArguments) {

    DOM lItemNodeArgument;
    try {
      lItemNodeArgument = FunctionUtils.getDOMNodeOrNull(pArguments, 0, "upload-type-info", false);
    }
    catch (XPathException e) {
      throw new ExInternal("Invalid node argument provided as first parameter to upload-type-info function", e);
    }

    //DOM does not exist, break out
    if(lItemNodeArgument == null) {
      return XdmEmptySequence.getInstance();
    }

    String lTypeArgument;
    try {
      //Static compilation should have checked that pArguments[1] is defined
      lTypeArgument = pArguments[1].getUnderlyingValue().head().getStringValue();
    }
    catch (XPathException e) {
      throw new ExInternal("Invalid string argument provided as second parameter to upload-type-info function", e);
    }

    //Resolve the node info -> file upload type
    ActionRequestContext lRequestContext = SaxonEnvironment.getThreadLocalRequestContext();
    NodeInfo lNodeInfo = lRequestContext.getCurrentModule().getNodeInfo(lItemNodeArgument);

    //No FSL attribute defined, return now to avoid getting the app's default FUT (this node is not an upload target)
    if(lNodeInfo.getFoxNamespaceAttribute(NodeAttribute.FILE_STORAGE_LOCATION) == null) {
      return XdmEmptySequence.getInstance();
    }

    FileUploadType lFileUploadType = lRequestContext.getModuleApp().getFileUploadType(lNodeInfo.getFoxNamespaceAttribute(NodeAttribute.UPLOAD_FILE_TYPE));

    //No file upload defined on node, return nothing
    if(lFileUploadType == null) {
      return XdmEmptySequence.getInstance();
    }

    List<String> lStringResults = new ArrayList<>();

    switch(lTypeArgument) {
      case "summary":
        lStringResults.add(lFileUploadType.getReadableSummaryDescription());
        break;
      case "min-size":
        lStringResults.add(lFileUploadType.getReadableMinSize());
        break;
      case "max-size":
        lStringResults.add(lFileUploadType.getReadableMaxSize());
        break;
      case "allowed-extensions":
        lStringResults.addAll(lFileUploadType.getAllowedExtensions());
        break;
      case "disallowed-extensions":
        lStringResults.addAll(lFileUploadType.getDisallowedExtensions());
        break;
      default:
        throw new ExInternal("Unrecognised info type '" + lTypeArgument + "', must be one of: summary, min-size, max-size, allowed-extensions, disallowed-extensions");
    }

    List<XdmItem> lItemResults = new ArrayList<>(lStringResults.size());
    for(String lStringResult : lStringResults) {
      if(lStringResult != null) {
        lItemResults.add(new XdmAtomicValue(lStringResult));
      }
    }

    return new XdmValue(lItemResults);
  }
}
