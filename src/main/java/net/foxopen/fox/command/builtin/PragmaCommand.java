/*

Copyright (c) 2010, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE -
                    ENERGY DEVELOPMENT UNIT (IT UNIT)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the DEPARTMENT OF ENERGY AND CLIMATE CHANGE nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

$Id$

*/
package net.foxopen.fox.command.builtin;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.FoxResponseCHAR;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ResponseOverride;
import net.foxopen.fox.thread.ActionRequestContext;


/**
 * Simple command that logs some message.
 *
 * @author Gary Watson
 */
public class PragmaCommand
extends BuiltInCommand {

  private static final Map gPragmaMap= new TreeMap();
  static {
    gPragmaMap.put("load-document-template", new String[] {"doc-template-name", "doc-template-target-metadata"});
    gPragmaMap.put("save-document-changes", new String[] {"doc-template-target-metadata"});
    gPragmaMap.put("set-html-response", new String[] {"target"});
    gPragmaMap.put("set-xml-response", new String[] {"target", "response-mime-type"});
  }
  private String mPragmaCmdIntern;
  private final Map mAttrMap;
  private DOM mCommandElement;

  /**
  * Contructs the command from the XML element specified.
  *
  * @param commandElement the element from which the command will
  *        be constructed.
  */
  public PragmaCommand(DOM pSourceCommandDOM)
  throws ExDoSyntax {
    super(pSourceCommandDOM);

    mCommandElement = pSourceCommandDOM;
    mPragmaCmdIntern = pSourceCommandDOM.getAttr("function").intern();

    // Validate function
    if(!gPragmaMap.containsKey(mPragmaCmdIntern)) {
      throw new ExDoSyntax("Unknown fm:pragma function=\""+mPragmaCmdIntern+"\"", pSourceCommandDOM);
    }
    mPragmaCmdIntern = mPragmaCmdIntern.intern();
    Set lValidAttrSet = new TreeSet(XFUtil.toArrayList(gPragmaMap.get(mPragmaCmdIntern)));
    lValidAttrSet.add("function");

    // Validate attributes
    mAttrMap = pSourceCommandDOM.getAttributeMap();
    Set lKeySet = new TreeSet(mAttrMap.keySet());
    lKeySet.removeAll(lValidAttrSet);
    if(!lKeySet.isEmpty()) {
      throw new ExDoSyntax("Command fm:pragma function=\""+mPragmaCmdIntern+"\" has invalid attributes: "+lKeySet.iterator().next(), pSourceCommandDOM);
    }
    lValidAttrSet.removeAll(mAttrMap.keySet());
    if(!lValidAttrSet.isEmpty()) {
      throw new ExDoSyntax("Command fm:pragma function=\""+mPragmaCmdIntern+"\" has missing attributes: "+lValidAttrSet.iterator().next(), pSourceCommandDOM);
    }

  }

  /**
  * Runs the command with the specified user thread and session.
  *
  * @param userThread the user thread context of the command
  * @return userSession the user's session context
  */
  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    ContextUElem pContextDOM = pRequestContext.getContextUElem();

    if(mPragmaCmdIntern=="flush-applications") {
      throw new ExInternal("flush-applications pragma not supported in FOX5");
    }
    else if (mPragmaCmdIntern == "accessibility-mode") {
      throw new ExInternal("accessibility-mode pragma not supported in FOX5");
    }
    else if(mPragmaCmdIntern=="dump-cached-apps") {
      throw new ExInternal("dump-cached-apps pragma not supported in FOX5");
    }
    else if (mPragmaCmdIntern=="set-audited-output-response") {
      throw new ExInternal("set-audited-output-response pragma not yet supported in FOX5");
    }
    else if (mPragmaCmdIntern=="remote-action-call") {
      throw new ExInternal("remote-action-call pragma not supported in FOX5");
    }
    else if(mPragmaCmdIntern=="save-document-changes") {
      // Save document snippet changes
      String lMetaDataXPATH = (String) mAttrMap.get("doc-template-target-metadata");
      DOM lMetaDataDOM;
      try {
        lMetaDataDOM = pContextDOM.extendedXPath1E(lMetaDataXPATH);
      }
      catch (ExActionFailed | ExCardinality e) {
        throw new ExInternal("Cardinality exception when saving document changes", e);
      }
      DOM lDummyDoc = DOM.createDocumentFromXMLString("<root/>"); // Dummy document used to clone the actual data item

      // Get all data items with a editable mm field
      DOMList lMMFieldDataList = null;
      try {
        lMMFieldDataList = lMetaDataDOM.xpathUL("/*/*/MM_FIELD_LIST/MM_FIELD[EDIT_FLAG != 'false']/DATA_LIST/DATA", null);
      } catch (ExBadPath e) {
        throw e.toUnexpected();
      }

      // Loop through the editable data items
      DOM lDataItem;
      while ((lDataItem = lMMFieldDataList.popHead()) != null) {
        String lActualDataItemRef = lDataItem.get1SNoEx("REF"); // Get current data items REF to actual value
        if (lActualDataItemRef.equals(""))
          throw new ExInternal("XPATH error: expected one match for xpath \"MM_FIELD/DATA_LIST/DATA/REF\"", mCommandElement);

        DOM lTempDataItemDOM; // Editable data from metadatas DATA_LIST/DATA structure
        try {
          lTempDataItemDOM = lDataItem.xpath1E("./*[name() = 'STRING' or name() = 'DATE' or name() = 'DATE_TIME' or name() = 'ANY_TYPE']");
        } catch (ExBadPath e) {
          throw e.toUnexpected();
        }
        catch (ExTooMany e) {
          throw e.toUnexpected("A data item should only have one occurrence of STRING, DATE, DATETIME, or ANY_TYPE elements.");
        }
        catch (ExTooFew e) {
          throw e.toUnexpected("A data item has no occurrence of STRING, DATE, DATETIME, or ANY_TYPE elements.");
        }

        //PN TO XTHREAD this new line has not been tested - previously called getAllUElemUnique() and cycled through looking for a DOM in each document seperately
        DOM lActualDataItemDOM = pContextDOM.getElemByRefOrNull(lActualDataItemRef); // Actual DOM which data should be copied back to

        // Check to see if a actual data item was returned
        if (lActualDataItemDOM != null) {
          String lActualDataStringValue;
          String lTempDataStringValue;
          String lDataType = lTempDataItemDOM.getName();
          if (lDataType.equals("ANY_TYPE")) {
            DOM lActualDataClone = lActualDataItemDOM.clone(true, lDummyDoc); // clone so we don't alter actual data definition
            lActualDataClone = lActualDataClone.removeRefsRecursive(); // remove foxids
            DOM lTempDataItemClone = lTempDataItemDOM.clone(true, lDummyDoc);
            lTempDataItemClone = lTempDataItemClone.removeRefsRecursive();

            // Pre-process actual complex type
            lActualDataStringValue = lActualDataClone.outputNodeToString();
            lActualDataStringValue = lActualDataStringValue.replaceAll("\\.  ", ". "); // remove double spaces for comparison
            // strip root element tags from data string
            lActualDataStringValue = lActualDataStringValue.substring(lActualDataStringValue.indexOf(">")+1, lActualDataStringValue.lastIndexOf("</") == -1 ? lActualDataStringValue.length() : lActualDataStringValue.lastIndexOf("</")  );

            // Pre-process editable complex type
            lTempDataStringValue = lTempDataItemClone.outputNodeToString();
            lTempDataStringValue = lTempDataStringValue.replaceAll("\\.  ", ". "); // remove double spaces for comparison
            // strip root element tags from data string
            lTempDataStringValue = lTempDataStringValue.substring(lTempDataStringValue.indexOf(">")+1, lTempDataStringValue.lastIndexOf("</") == -1 ? lTempDataStringValue.length() : lTempDataStringValue.lastIndexOf("</") );
          }
          else {
            //TODO PN changed - this used to pretty print but now does not - impact assessment required
            lActualDataStringValue = lActualDataItemDOM.outputNodeContentsToString(false);
            lTempDataStringValue = lTempDataItemDOM.outputNodeContentsToString(false);
          }

          if (!lActualDataStringValue.equals(lTempDataStringValue)) { // Check to see whether actual value is different to editable value
            lActualDataItemDOM.removeAllChildren();
            lTempDataItemDOM.copyContentsTo(lActualDataItemDOM); // copy from DATA leg to element with correct fox_id

            // Check to see if change causes actual data item to be same as the template default value
            DOM lDefaultValue = lDataItem.get1EOrNull("../../VALUE");
            String lDefaultTemplateData = null;
            if (lDefaultValue != null) {
              lDefaultValue = lDefaultValue.clone(true, lDummyDoc).removeRefsRecursive();
              if (lDataType.equals("ANY_TYPE")) {
                // Pre-process default complex type
                lDefaultTemplateData = lDefaultValue.outputNodeToString();
                // remove double spaces for comparison and strip off the surrounding root element
                lDefaultTemplateData = lDefaultTemplateData.replaceAll("\\.  ", ". ");
                lDefaultTemplateData = lDefaultTemplateData.substring(lDefaultTemplateData.indexOf(">")+1, lDefaultTemplateData.lastIndexOf("</") == -1 ? lDefaultTemplateData.length() : lDefaultTemplateData.lastIndexOf("</") );
              }
              //TODO PN changed - this used to pretty print but now does not - impact assessment required
              else lDefaultTemplateData = lDefaultValue.outputNodeContentsToString(false);
            }

            if (lDefaultValue == null || !lTempDataStringValue.equals(lDefaultTemplateData)) {
              lActualDataItemDOM.setAttr("docgen-modified-datetime", XFUtil.formatDatetimeXML(new Date())); // data has changed so set modified attribute
            }
            else { // TODO ***AP*** this else can be removed when fieldScope is enhanced to better handle the setting of parameters
              lActualDataItemDOM.removeAttr("docgen-modified-datetime"); // remove modified attribute as value is same as default.  possible revert took place
            }
          }
          // else values are the same and no need to copy over
        }
      }
    }
    else if(mPragmaCmdIntern=="load-document-template") {
      // Load document template data
      //TODO AT DOCGEN
      //Constructor.pragmaCommandLoadDocTemplate(pContextDOM, pContextUCon, mAttrMap, null);
    }
    else if (mPragmaCmdIntern=="set-html-response") {
      // Build up a buffer containing the response HTML
      String xpathTarget = (String) mAttrMap.get("target");
      DOM lHtml;
      try {
        lHtml = pContextDOM.extendedXPath1E(xpathTarget, false);
      }
      catch (ExActionFailed | ExCardinality e) {
        throw new ExInternal("Error when locating target for HTML response", e);
      }

      StringBuffer lHtmlStringBuffer = new StringBuffer(lHtml.outputNodeToString(false));

      //Set the override response as an XDoResult to be picked up before HTML gen
      FoxResponseCHAR lHTMLResponse = new FoxResponseCHAR("text/html; charset=UTF-8", lHtmlStringBuffer, -1);

      pRequestContext.addXDoResult(new ResponseOverride(lHTMLResponse));
    }
    else if (mPragmaCmdIntern=="set-xml-response") {
      // Build up a buffer containing the response XML
      String xpathTarget = (String) mAttrMap.get("target");

      DOMList lXmlDOMList;
      try {
        lXmlDOMList = pContextDOM.extendedXPathUL(xpathTarget, null);
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Error when locating target for XML response", e);
      }

      DOM lXmlDOM;
      boolean lRootNodeFound = false;
      for(int i=0; i<lXmlDOMList.getLength(); i++) {
        lXmlDOM=lXmlDOMList.item(i);
        DOM.NodeType lInternedNodeType = lXmlDOM.nodeType();
        if(lInternedNodeType == DOM.NodeType.ELEMENT) {
          if(lRootNodeFound) {
            throw new ExInternal("target match contains more than one top level element");
          } else {
            lRootNodeFound=true;
          }
        }
        //PN: This code previously removed the XML declaration PI, but this has been removed as XOM will not return
        //it as a PI
      }

      DOM lResponseContainer = DOM.createDocument("xml-response-container");
      lXmlDOMList.copyContentsTo(lResponseContainer);

      StringBuffer lXmlStringBuffer = new StringBuffer(lResponseContainer.outputNodeContentsToString(true, true));

      //Set the override response as an XDoResult to be picked up before HTML gen
      FoxResponseCHAR lXMLResponse = new FoxResponseCHAR((String) mAttrMap.get("response-mime-type") + "; charset=UTF-8", lXmlStringBuffer, -1);

      pRequestContext.addXDoResult(new ResponseOverride(lXMLResponse));
    }
    else {
      // Unknown pragma command
      throw new ExInternal("pragma command logic error");
    }

    return XDoControlFlowContinue.instance();
  }

  public boolean isCallTransition() {
    return false;
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new PragmaCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("pragma");
    }
  }
}
