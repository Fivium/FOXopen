package net.foxopen.fox.command.builtin;

import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.Collection;
import java.util.Collections;


/**
 * Implementation of an XSL Transformation command. This command
 * can transform source data at a specified storage location
 * or data DOm XPath, using a style sheet at a storage location
 * or URI.
 *
 * <p>The resulting XML document can be targeted to a specified storage
 * location or data DOM XPath.
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
   * Constructs a transform command from the XML element specified.
   *
   * @param commandElement the element from which the command will
   *        be constructed.
   */
  private XSLTransformCommand(DOM commandElement) {
    super(commandElement);

    sourceXPath = getAttribute("source-xml-xpath");
    sourceXSLURI = getAttribute("source-xsl-URI");
    targetXPath = getAttribute("target-xpath");

    if (sourceXPath == null) {
      throw new ExInternal("Error parsing "+getName()+" command - no source "+
                           "XPath specified. Refer to the user guide for usage information.");
    }

    if (sourceXSLURI == null) {
      throw new ExInternal("Error parsing "+getName()+" command - no source XSLT style sheet "+
                           "URI specified. The URI can refer internally, to a FOX component, or an "+
                           "external resource in the form of a URL. Refer to the user guide for usage information.");
    }

    if (targetXPath == null) {
      throw new ExInternal("Error parsing "+getName()+" command - no target "+
                           "XPath specified. Refer to the user guide for usage information.");
    }
  }

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
