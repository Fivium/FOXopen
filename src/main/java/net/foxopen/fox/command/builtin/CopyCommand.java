package net.foxopen.fox.command.builtin;

import net.foxopen.fox.ContextUCon;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.datanode.NodeInfo;
import net.foxopen.fox.module.mapset.MapSet;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.track.Track;

import java.util.Collection;
import java.util.Collections;


public class CopyCommand
extends BuiltInCommand {

  private final String mFromXPath;
  private final String mToXPath;
  private final String mMaterialiseMapsetsXPath;


  private CopyCommand(DOM pDOM)
  throws ExInternal {
    super(pDOM);
    mFromXPath = pDOM.getAttr("from");
    mToXPath = pDOM.getAttr("to");
    mMaterialiseMapsetsXPath = pDOM.getAttr("materialise-mapsets");
  }

  @Override
  public boolean isCallTransition() {
    return false;
  }

  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    ContextUElem lContextUElem = pRequestContext.getContextUElem();
    ContextUCon lContextUCon = pRequestContext.getContextUCon();

    DOMList sourceNodes;
    try {
      sourceNodes = lContextUElem.extendedXPathUL(mFromXPath, ContextUElem.ATTACH);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to evaluate 'from' XPath of fm:copy command", e);
    }

    DOMList targetNodes;
    try {
      targetNodes = lContextUElem.extendedXPathUL(mToXPath, ContextUElem.ATTACH);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to evaluate 'to' XPath of fm:copy command", e);
    }

    boolean lMaterialiseMapsets = false;
    if (!XFUtil.isNull(mMaterialiseMapsetsXPath)) {
      try {
        lMaterialiseMapsets = lContextUElem.extendedXPathBoolean(lContextUElem.attachDOM(), mMaterialiseMapsetsXPath);
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Failed to evaluate 'materialise-mapsets' XPath of fm:copy command", e);
      }
    }

    for (int s = 0; s < sourceNodes.getLength(); s++) {
      DOM source = sourceNodes.item(s);

      for (int t = 0; t < targetNodes.getLength(); t++) {
        DOM target = targetNodes.item(t);
        DOM copyElem = source.copyToParent(target);

        // Mapset materialising
        if (lMaterialiseMapsets) {
          // Recurse through the source dom, finding mapset data values to replace
          try {
            materialseMapSetValues(pRequestContext, source, copyElem, lContextUElem, lContextUCon);
          }
          catch (ExActionFailed e) {
          }
        }
      }
    }

    pRequestContext.addSysDOMInfo("last-command/copycount", Integer.toString(sourceNodes.getLength()));

    return XDoControlFlowContinue.instance();
  }

  /**
   * Take a SourceDOM and DestinationDOM of identical values, hunt for mapsets associated with the SourceDOM and replace
   * the values of the corresponding nodes in the DestinationDOM with the values defined in the mapset
   *
   * @param pSourceDOM Original source DOM
   * @param pDestinationDOM DOM source DOM was copied to
   * @param pDOMContext Context to use
   * @param pContextUCon ContextUCon to use
   * @throws ExActionFailed
   */
  private void materialseMapSetValues(ActionRequestContext pRequestContext, DOM pSourceDOM, DOM pDestinationDOM, ContextUElem pDOMContext, ContextUCon pContextUCon) throws ExActionFailed {
    // Try and get a NodeInfo object for the source node
    NodeInfo lSourceNodeInfo = pRequestContext.getCurrentModule().getNodeInfo(pSourceDOM);
    if (lSourceNodeInfo == null) {
      // Can't do any processing if there's no NodeInfo for the node
      Track.info("fm:copy", "Copy found with materialise-mapsets but source DOM not marked up in module schema: " + pSourceDOM.absolute());
      return;
    }
    else {
      // If we got a NodeInfo, try and find a map-set reference
      String lMapSetName = lSourceNodeInfo.getAttribute("fox", "map-set");
      if (!XFUtil.isNull(lMapSetName)) {
        MapSet lMapSet = pRequestContext.resolveMapSet(lMapSetName, pSourceDOM, lSourceNodeInfo.getAttribute("fox", "map-set-attach"));

        String lMultiSelectSubNode = lSourceNodeInfo.getAttribute("fox", "selector");
        if (!XFUtil.isNull(lMultiSelectSubNode)) {
          // If we have a multi-select mapset, recurse through its children that have the name in the selector attribute
          for (int lChildIndex = 0; lChildIndex < pSourceDOM.getChildNodes().getLength(); lChildIndex++) {
            DOM lSourceMultiSelectSubNode = pSourceDOM.getChildNodes().item(lChildIndex);

            if (!lMultiSelectSubNode.equals(lSourceMultiSelectSubNode.getName())) {
              // Skip node if it's not the "selector" node of a multi-select
              continue;
            }

            // Replace the mapset data with the value in the destination node based on the source node and the MapSet
            replaceMapsetData(lSourceMultiSelectSubNode, pDestinationDOM.getChildNodes().item(lChildIndex), lMapSet);
          }
        }
        else {
          // Replace the mapset data with the value in the destination DOM based on the source DOM and the MapSet
          replaceMapsetData(pSourceDOM, pDestinationDOM, lMapSet);
        }
      }
      else if ("xs:boolean".equals(lSourceNodeInfo.getDataType())) {
        if ("true".equalsIgnoreCase(pSourceDOM.outputNodeContentsToString(false))) {
          String lKeyTrue = XFUtil.nvl(lSourceNodeInfo.getAttribute("fox", "key-true"), "Yes");

          try {
            lKeyTrue = pDOMContext.extendedStringOrXPathString(pSourceDOM, lKeyTrue);
          }
          catch (ExActionFailed e) {
            throw e.toUnexpected("Failed to evaluate key-true attribue");
          }

          pDestinationDOM.setText(lKeyTrue);
        }
        else if ("false".equalsIgnoreCase(pSourceDOM.outputNodeContentsToString(false))) {
          String lKeyFalse = XFUtil.nvl(lSourceNodeInfo.getAttribute("fox", "key-false"), "No");

          try {
            lKeyFalse = pDOMContext.extendedStringOrXPathString(pSourceDOM, lKeyFalse);
          }
          catch (ExActionFailed e) {
            throw e.toUnexpected("Failed to evaluate key-false attribue");
          }

          pDestinationDOM.setText(lKeyFalse);
        }
      }
      else {
        // If the node wasn't a mapset, recurse through its children looking for mapset values to materialise
        for (int lChildIndex = 0; lChildIndex < pSourceDOM.getChildNodes().getLength(); lChildIndex++) {
          materialseMapSetValues(pRequestContext, pSourceDOM.getChildNodes().item(lChildIndex), pDestinationDOM.getChildNodes().item(lChildIndex), pDOMContext, pContextUCon);
        }
      }
    }
  }

  /**
   * Replace the text in pDestinationDOM with the pMapSet value corresponding to pSourceDOM
   *
   * @param pSourceDOM Original mapset data
   * @param pDestinationDOM Destination of mapset data
   * @param pMapSet Mapset to use to find out the value for pDestinationDOM using pSourceDOM as the key
   * @throws ExActionFailed
   */
  private void replaceMapsetData(DOM pSourceDOM, DOM pDestinationDOM, MapSet pMapSet)
  throws ExActionFailed {

    int lMapSetItemIndex = pMapSet.indexOf(pSourceDOM);
    if(lMapSetItemIndex >= 0) {
      //If the mapset contains this DOM as an item, look up the key and and set it as the text of the destination node
      pDestinationDOM.removeAllChildren();
      String lMsKey = pMapSet.getEntryList().get(lMapSetItemIndex).getKey();
      pDestinationDOM.setText(lMsKey);
    }
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new CopyCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("copy");
    }
  }
}
