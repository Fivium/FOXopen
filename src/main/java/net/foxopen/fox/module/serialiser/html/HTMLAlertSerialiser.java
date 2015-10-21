package net.foxopen.fox.module.serialiser.html;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedBufferPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.thread.alert.AlertMessage;
import net.foxopen.fox.thread.alert.BasicAlertMessage;
import net.foxopen.fox.thread.alert.BufferAlertMessage;
import net.foxopen.fox.thread.alert.RichAlertMessage;
import net.foxopen.fox.thread.alert.RichTextAlertMessage;
import org.apache.commons.lang3.StringEscapeUtils;
import org.json.simple.JSONObject;

import java.util.List;

/**
 * Methods for serialising the various types of fm:alert for an HTML generator output.
 */
public class HTMLAlertSerialiser {

  /**
   * Inserts JavaScript for creating all {@link AlertMessage}s registered on the SerialisationContext into the given HTMLSerialiser.
   * If no alert messages are registered, no action is taken. Alerts are displayed in the order of registration.<br><br>
   * Note: this method assumes it is writing into an open &lt;script&gt; tag on the HTMLSerialiser.
   * @param pSerialiser Current HTMLSerialiser.
   * @param pSerialisationContext Current SerialisationContext.
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

  /**
   * Serialises the content of any buffer-based alerts into hidden divs with predetermined IDs. These IDs are referenced when
   * the JavaScript for displaying the alert is serialised.
   * @param pSerialiser Current HTMLSerialiser.
   * @param pSerialisationContext Current SerialisationContext.
   */
  public static void insertAlertBuffers(HTMLSerialiser pSerialiser, SerialisationContext pSerialisationContext) {

    List<BufferAlertMessage> lBufferAlertMessages = pSerialisationContext.getXDoResultList(BufferAlertMessage.class);

    for (BufferAlertMessage lAlert : lBufferAlertMessages) {
      pSerialiser.append("<div id=\"");
      pSerialiser.append(containerIdForBufferAlert(lAlert));
      pSerialiser.append("\" style=\"display: none;\">");
      pSerialiser.append("<div class=\"modal-popover-icon\"></div>");
      pSerialiser.append("<div class=\"modal-popover-text\">");

      DOM lBufferAttach = pSerialisationContext.getContextUElem().getElemByRef(lAlert.getBufferAttachFoxId());

      EvaluatedBufferPresentationNode lBuffer = pSerialisationContext.evaluateBuffer(lAlert.getBufferName(), lBufferAttach);
      pSerialiser.getComponentBuilder(lBuffer.getPageComponentType()).buildComponent(pSerialisationContext, pSerialiser, lBuffer);

      pSerialiser.append("</div></div>");
    }
  }

  /**
   * If a title property is present in the given property map, does an in-place replacement with any HTML escaped.
   * @param pMessageProperties Property object to be modified.
   */
  private static void escapeTitle(JSONObject pMessageProperties) {
    String lTitle = (String) pMessageProperties.get(RichAlertMessage.TITLE_JSON_PROPERTY_NAME);
    if(!XFUtil.isNull(lTitle)) {
      pMessageProperties.put(RichAlertMessage.TITLE_JSON_PROPERTY_NAME, StringEscapeUtils.escapeHtml4(lTitle));
    }
  }

  /**
   * Inserts JS for creating a basic/native JS alert.
   * @param pSerialiser Current HTMLSerialiser.
   * @param pBasicAlertMessage Alert to be inserted.
   */
  private static void insertBasicAlert(HTMLSerialiser pSerialiser, BasicAlertMessage pBasicAlertMessage) {

    String lAlertMessage = pBasicAlertMessage.getMessage();
    JSONObject lMessageProperties = new JSONObject();

    lMessageProperties.put("alertType", "native");
    //String escaping should be handled by JSON serialiser - FOXalert JS explicitly converts "\n" strings to newline characters
    lMessageProperties.put("message", lAlertMessage);

    enqueueAlert(pSerialiser, lMessageProperties);
  }

  /**
   * Inserts JS for creating an HTML alert.
   * @param pSerialiser Current HTMLSerialiser.
   * @param pRichTextAlertMessage Alert to be inserted.
   */
  private static void insertRichTextAlert(HTMLSerialiser pSerialiser, RichTextAlertMessage pRichTextAlertMessage) {

    JSONObject lMessageProperties = pRichTextAlertMessage.getJSONPropertyObject();

    String lMessage = pRichTextAlertMessage.getMessage();

    lMessage = lMessage.replace("\\n", "##SAFE_ESCAPE_LINEBREAK##");

    //Only escape HTML if required - otherwise, HTML can be passed through direct from module markup
    if(pRichTextAlertMessage.isEscapingRequired()) {
      lMessage = StringEscapeUtils.escapeHtml4(lMessage);
    }

    //Support conversion of "\n" strings into line breaks
    lMessage = lMessage.replace("##SAFE_ESCAPE_LINEBREAK##", "<br>");

    escapeTitle(lMessageProperties);

    lMessageProperties.put("alertType", "text");
    lMessageProperties.put("message", lMessage);

    enqueueAlert(pSerialiser, lMessageProperties);
  }

  /**
   * Inserts JS for creating a buffer based alert.
   * @param pSerialiser Current HTMLSerialiser.
   * @param pBufferAlertMessage  Alert to be inserted.
   */
  private static void insertBufferAlert(HTMLSerialiser pSerialiser, BufferAlertMessage pBufferAlertMessage) {

    JSONObject lMessageProperties = pBufferAlertMessage.getJSONPropertyObject();

    escapeTitle(lMessageProperties);

    //Point JS to the div for this alert based on unique ID - the div should have been serialised by insertAlertBuffers()
    lMessageProperties.put("alertType", "buffer");
    lMessageProperties.put("bufferId", containerIdForBufferAlert(pBufferAlertMessage));

    enqueueAlert(pSerialiser, lMessageProperties);
  }

  /**
   * @param pBufferAlertMessage Buffer alert being serialised.
   * @return Gets the HTML ID attribute of a container div for a serialised alert buffer.
   */
  private static String containerIdForBufferAlert(BufferAlertMessage pBufferAlertMessage) {
    return "alertBuffer-" + pBufferAlertMessage.getMessageId();
  }

  /**
   * Common method for writing JS to show an onLoad alert. These must be queued up so multiple alerts do not overlap each other.
   * @param pSerialiser Current HTMLSerialiser.
   * @param pAlertProperties Alert JSON properties.
   */
  private static void enqueueAlert(HTMLSerialiser pSerialiser, JSONObject pAlertProperties) {
    pSerialiser.append("FOXalert.enqueueOnLoadAlert("  +  pAlertProperties.toJSONString() + ");\n");
  }
}
