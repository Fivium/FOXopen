package net.foxopen.fox.module.serialiser.components.html;

import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedExternalURLPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import org.apache.commons.lang3.StringEscapeUtils;

/**
 * Builds an external URL with pre-evaluated attributes from an EvaluatedExternalURLPresentationNode
 */
public class ExternalURLComponentBuilder extends ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> {
  private static final ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> INSTANCE = new ExternalURLComponentBuilder();
  public static final String NON_JS_LINK_TYPE = "non-js";

  public static final ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> getInstance() {
    return INSTANCE;
  }

  private ExternalURLComponentBuilder () {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedPresentationNode pEvalNode) {
    EvaluatedExternalURLPresentationNode lExternalURLNode = (EvaluatedExternalURLPresentationNode)pEvalNode;

    pSerialiser.append("<a ");

    String lURI = pSerialisationContext.getStaticResourceOrFixedURI(lExternalURLNode.getHRef());
    String lLinkType = lExternalURLNode.getType();

    // Choose between a non-js link and a FOXjs.openwin() windowOptions string
    if (NON_JS_LINK_TYPE.equals(lLinkType)) {
      pSerialiser.append("href=\"");
      pSerialiser.append(StringEscapeUtils.escapeHtml4(lURI));
      pSerialiser.append("\" target=\"_blank\" rel=\"noopener noreferrer\"");
    }
    else {
      pSerialiser.append("href=\"#\" onclick=\"FOXjs.openwin({url:'");
      pSerialiser.append(StringEscapeUtils.escapeHtml4(lURI));
      pSerialiser.append("',windowOptions:'");
      pSerialiser.append(StringEscapeUtils.escapeHtml4(lLinkType));
      pSerialiser.append("'});return false;\" title=\"");
      pSerialiser.append(StringEscapeUtils.escapeHtml4(lExternalURLNode.getTitle()));
      pSerialiser.append("\"");
    }

    pSerialiser.append(">");
    pSerialiser.append(StringEscapeUtils.escapeHtml4(lExternalURLNode.getLinkText()));
    pSerialiser.append("</a>");
  }
}
