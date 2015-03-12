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

import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;


/**
 * Implementation of an XSL Transformation command. This command
 * can transform source data at a specified storage location
 * or data DOm XPath, using a style sheet at a storage location
 * or URI.
 *
 * <p>The resulting XML document can be targetted to a specified storage
 * location or data DOM XPath.
 *
 * @author Gary Watson
 */
public class XSLTransformCommand
extends BuiltInCommand
{
   /** The source xpath. */
   private final String sourceXPath;

   /** The source URI - can be internal to the web app or external. */
   private final String sourceXSLURI;

   /** The target XPath. */
   private final String targetXPath;


   /**
   * Contructs a transform command from the XML element specified.
   *
   * @param commandElement the element from which the command will
   *        be constructed.
   */
  private XSLTransformCommand(DOM commandElement) {
    super(commandElement);

     sourceXPath = getAttribute("source-xml-xpath");
     sourceXSLURI = getAttribute("source-xsl-URI");
     targetXPath = getAttribute("target-xpath");

     if (sourceXPath == null)
     {
        throw new ExInternal("Error parsing "+getName()+" command - no source "+
                             "XPath specified. Refer to the user guide for usage information.");
     }

     if (sourceXSLURI == null)
     {
        throw new ExInternal("Error parsing "+getName()+" command - no source XSLT style sheet "+
                             "URI specified. The URI can refer internally, to a FOX component, or an "+
                             "external resource in the form of a URL. Refer to the user guide for usage information.");
     }

     if (targetXPath == null)
     {
        throw new ExInternal("Error parsing "+getName()+" command - no target "+
                             "XPath specified. Refer to the user guide for usage information.");
     }
   }

//   /**
//   * Runs the command with the specified user thread and session.
//   *
//   * @param userThread the user thread context of the command
//   * @return userSession the user's session context
//   */
//  public void run (XThread userThread,
//                   ContextUElem contextUElem,
//                   ContextUCon contextUCon)
//    throws ExInternal, ExActionFailed
//  {
//      //------------------------------------------------------------------------
//      // Obtain the source node, which could be a data DOM node or a file-type node.
//      //------------------------------------------------------------------------
//    DOM sourceNode;
//    try {
//      sourceNode = contextUElem.extendedXPath1E(sourceXPath);
//    }
//    catch (ExCardinality e) {
//      throw new ExActionFailed("BADPATH", "Cardinality exception when evaluating source XPath", e);
//    }
//    Mod module              = userThread.getTopModule();
//    NodeInfo sourceNodeInfo = module.getNodeInfo(sourceNode);
//      //TODO - This used to read from an uploaded file which might be useful
//
//
//    InputStream sourceXSLIS = null;
//      try
//      {
//         //------------------------------------------------------------------------
//         // Obtain the source XSLT style sheet, which could be URI to a fox component
//         // or a URL to an external resource.
//         //------------------------------------------------------------------------
//         try
//         {
//            URL componentURL = new URL(sourceXSLURI); // See if it is a valid URL
//
//            try
//            {
//               sourceXSLIS = componentURL.openStream();
//            }
//            catch (Throwable unexpectedEx)
//            {
//               throw new ExActionFailed("INVALIDXSLSS", "The XSL Style Sheet document, url="+componentURL+", is invalid or could not be read.", unexpectedEx);
//            }
//         }
//         catch (MalformedURLException ex)
//         {
//            try
//            {
//               FoxComponent foxComponent = userThread.getTopApp().getComponent(sourceXSLURI);
//               if ( !(foxComponent instanceof ComponentText) )
//               {
//                  throw new ExInternal("Expected a MIME type of text/* (text/xsl preferably) for resource, "+sourceXSLURI+
//                                       "! Actually, the MIME type reported was "+foxComponent.getType()+" which is "+
//                                       "not wrapped by ComponentText type and therefore not deemed to be textual in content.");
//
//               }
//               sourceXSLIS = new StringBufferInputStream(((ComponentText)foxComponent).getText().toString());
//            }
//            catch (Throwable unexpectedEx)
//            {
//               throw new ExActionFailed("INVALIDXSLSS", "The XSL Style Sheet document, application component uri="+sourceXSLURI+", is invalid or could not be read.", unexpectedEx);
//            }
//         }
//
//         try
//         {
//   //         DOM resultDOM = XSLTransformerUtil.transformToDOM(xslDOM, xmlDOM, false);
//            DOM targetDOM = contextUElem.extendedXPath1E(targetXPath);
//        XSLTransformerUtil.transformToDOM(sourceNode, contextUElem.attachDOM(), sourceXSLIS, targetDOM, contextUElem);
//
//
//         }
//         catch (Throwable ex)
//         {
//            throw new ExActionFailed("TRANFORMERROR",
//                                     "Error transforming XML content, XML source=\""+sourceXPath+"\", XSL source=\""+sourceXSLURI+
//                                     "\" - see nested exception for further information.", ex);
//         }
//      }
//      finally
//      {
//         IOUtil.closeIgnoringErrors(sourceXSLIS);
//      }
//
//   }
//
//   private InputStream getInputStreamForFileTypeElement(XThread userThread,
//                                                        ContextUElem contextUElem,
//                                                        ContextUCon contextUCon,
//                                                        DOM sourceNode) throws ExActionFailed
//   {
//      Mod module                           = userThread.getTopModule();
//      NodeInfo sourceNodeInfo              = module.getNodeInfo(sourceNode);
//      WorkingFileStorageLocation workingSL   = null;
//      String connectKey                    = null;
//      InputStream sourceIS                 = null;
//
//      String fileStorageLocation = sourceNodeInfo.getAttribute("fox", "file-storage-location");
//      boolean isSourceStorageLocationBased = fileStorageLocation != null;
//
//      try
//      {
//         if (isSourceStorageLocationBased)
//         {
//            FileStorageLocation sl = module.getFileStorageLocation(sourceNodeInfo.getAttribute("fox", "file-storage-location"));
//            if (sl == null)
//            {
//               throw new ExInternal("Unable to locate storage location, \""+fileStorageLocation+"\", in module \""+module.getName()+"\" - "+
//                                    "please update the module specification and ensure one exists.");
//            }
//
//            DOM currrentAttachPoint = contextUElem.getUElem(ContextLabel.ATTACH);
//            DOM currrentActionPoint = contextUElem.getUElem(ContextLabel.ACTION);
//            try
//            {
//               contextUElem.setUElem(ContextLabel.ATTACH, sourceNode.getParentOrSelf());
//               contextUElem.setUElem(ContextLabel.ACTION, sourceNode.getParentOrSelf());
//               workingSL = new WorkingFileStorageLocation(sl, contextUElem, false);
//            }
//            finally
//            {
//               // Restore previous attach points
//               contextUElem.setUElem(ContextLabel.ATTACH, currrentAttachPoint);
//               contextUElem.setUElem(ContextLabel.ACTION, currrentActionPoint);
//            }
//            // TODO AT [ConnectKey]
//            connectKey = userThread.getTopApp().getConnectKey();
//
//            try
//            {
//               ByteArrayOutputStream baos = new ByteArrayOutputStream(); // TODO: Need to bring into memory
//              //TODO PN WFSL
//               //workingSL.streamFile(baos, contextUCon, true);
//               sourceIS = new ByteArrayInputStream(baos.toByteArray());
//               contextUCon.getUCon().commit();
//            }
//            catch(ExRoot ex)
//            {
//               try
//               {
//                 if (contextUCon != null)
//                 contextUCon.getUCon().rollback();
//               }
//               catch (Throwable th)
//               {
//                  throw new ExActionFailed("TODOEX", "Unexpected error caught while finishing XSLT source stream:", th);
//               }
//            }
//            finally
//            {
//               if (contextUCon != null)
//               {
//                 contextUCon.getUCon().closeNoRecycle();
//               }
//            }
//         }
//         else
//         {
//            sourceIS  = new ByteArrayInputStream(Base64.decodeBase64(sourceNode.get1S("content").getBytes()));
//         }
//
//         return sourceIS;
//      }
//      catch (ExRoot ex)
//      {
//         throw new ExActionFailed("TODOEX", "Error locating/finding source XMl for XSLT transformation, module="+module.getName()+":", ex);
//      }
//   }


  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {
    throw new ExInternal("XSLTransform command not implemented in FOX5");
  }

  public boolean isCallTransition() {
   return false;
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new XSLTransformCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("XSLTransform");
    }
  }

}
