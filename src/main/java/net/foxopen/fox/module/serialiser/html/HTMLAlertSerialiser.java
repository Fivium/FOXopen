package net.foxopen.fox.module.serialiser.html;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedBufferPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.thread.alert.AlertMessage;
import net.foxopen.fox.thread.alert.BasicAlertMessage;
import net.foxopen.fox.thread.alert.BufferAlertMessage;
import net.foxopen.fox.thread.alert.RichTextAlertMessage;
import org.apache.commons.lang3.StringEscapeUtils;
import org.json.simple.JSONObject;

import java.util.List;

/**
 * Methods for serialising the various types of fm:alert for an HTML generator output.
 */
public class HTMLAlertSerialiser {

  /**
   * Get alert messages from the EvaluatedParseTree and serialise them for JS alerts
   * @param pSerialiser
   * @param pSerialisationContext
   */
  public static void insertAlerts(HTMLSerialiser pSerialiser, SerialisationContext pSerialisationContext) {
    List<AlertMessage> lAlertMessages = pSerialisationContext.getAlertMessages();

    for(AlertMessage lMessage : lAlertMessages) {

      if(lMessage instanceof BasicAlertMessage) {
        insertBasicAlert(pSerialiser, (BasicAlertMessage) lMessage);
      }
      else if(lMessage instanceof RichTextAlertMessage) {
        insertRichTextAlert(pSerialiser, (RichTextAlertMessage) lMessage);
      }
      else if(lMessage instanceof BufferAlertMessage) {
        insertBufferAlert(pSerialiser, (BufferAlertMessage) lMessage);
      }
      else {
        throw new ExInternal("Cannot insert an alert of type " + lMessage.getClass().getName());
      }
    }

    if(lAlertMessages.size() > 0) {
      pSerialiser.append("FOXalert.processNextOnLoadAlert();\n");
    }
  }

  public static void insertAlertBuffers(HTMLSerialiser pSerialiser, SerialisationContext pSerialisationContext) {

    List<BufferAlertMessage> lBufferAlertMessages = pSerialisationContext.getXDoResultList(BufferAlertMessage.class);

    for (BufferAlertMessage lAlert : lBufferAlertMessages) {
      pSerialiser.append("<div id=\"");
      pSerialiser.append(containerIdForBufferAlert(lAlert));
      pSerialiser.append("\" style=\"display: none;\">");

      DOM lBufferAttach = pSerialisationContext.getContextUElem().getElemByRef(lAlert.getBufferAttachFoxId());

      EvaluatedBufferPresentationNode lBuffer = pSerialisationContext.evaluateBuffer(lAlert.getBufferName(), lBufferAttach);
      pSerialiser.getComponentBuilder(lBuffer.getPageComponentType()).buildComponent(pSerialisationContext, pSerialiser, lBuffer);

      pSerialiser.append("</div>");
    }
  }

  private static void insertBasicAlert(HTMLSerialiser pSerialiser, BasicAlertMessage pBasicAlertMessage) {

    String lAlertMessage = pBasicAlertMessage.getMessage();
    lAlertMessage = lAlertMessage.replaceAll("\\\\n", "##SAFE_ESCAPE_LINEBREAK##");
    lAlertMessage = StringEscapeUtils.escapeEcmaScript(lAlertMessage);
    lAlertMessage = lAlertMessage.replaceAll("##SAFE_ESCAPE_LINEBREAK##", "\\\\n");

    JSONObject lMessageProperties = new JSONObject();

    lMessageProperties.put("alertType", "native");
    lMessageProperties.put("message", lAlertMessage);

    enqueueAlert(pSerialiser, lMessageProperties);
  }

  private static void insertRichTextAlert(HTMLSerialiser pSerialiser, RichTextAlertMessage pRichTextAlertMessage) {

    JSONObject lMessageProperties = pRichTextAlertMessage.getJSONPropertyObject();

    String lMessage = pRichTextAlertMessage.getMessage();

    lMessage = lMessage.replaceAll("\\\\n", "##SAFE_ESCAPE_LINEBREAK##");

    if(pRichTextAlertMessage.isEscapingRequired()) {
      lMessage = StringEscapeUtils.escapeHtml4(lMessage);
    }

    lMessage = lMessage.replaceAll("##SAFE_ESCAPE_LINEBREAK##", "<br>");

    lMessageProperties.put("alertType", "text");
    lMessageProperties.put("message", lMessage);

    enqueueAlert(pSerialiser, lMessageProperties);
  }

  private static void insertBufferAlert(HTMLSerialiser pSerialiser, BufferAlertMessage pBufferAlertMessage) {

    JSONObject lMessageProperties = pBufferAlertMessage.getJSONPropertyObject();

    lMessageProperties.put("alertType", "buffer");
    lMessageProperties.put("bufferId", containerIdForBufferAlert(pBufferAlertMessage));

    enqueueAlert(pSerialiser, lMessageProperties);
  }

  private static String containerIdForBufferAlert( BufferAlertMessage pBufferAlertMessage) {
    return "alertBuffer-" + pBufferAlertMessage.getMessageId();
  }

  private static void enqueueAlert(HTMLSerialiser pSerialiser, JSONObject pAlertProperties) {
    pSerialiser.append("FOXalert.enqueueOnLoadAlert("  +  pAlertProperties.toJSONString() + ");\n");
  }
}
