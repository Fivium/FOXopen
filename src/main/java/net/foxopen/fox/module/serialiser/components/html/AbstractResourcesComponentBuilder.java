package net.foxopen.fox.module.serialiser.components.html;

import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;

/**
 * Shared functionality for the Header and Footer component builders.
 */
public abstract class AbstractResourcesComponentBuilder
extends ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode>  {

  protected AbstractResourcesComponentBuilder() {}

  protected static void javascript(HTMLSerialiser pSerialiser, String pPath) {
    pSerialiser.append("<script src=\"");
    pSerialiser.append(pPath);
    pSerialiser.append("\" type=\"text/javascript\"></script>");
  }

  protected static void css(HTMLSerialiser pSerialiser, String pPath) {
    pSerialiser.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
    pSerialiser.append(pPath);
    pSerialiser.append("\">");
  }

  protected static void css(HTMLSerialiser pSerialiser, String pPath, String pBrowserCondition) {
    pSerialiser.append("<!--[if ");
    pSerialiser.append(pBrowserCondition);
    pSerialiser.append("]>");
    css(pSerialiser, pPath);
    pSerialiser.append("<![endif]-->");
  }
}
