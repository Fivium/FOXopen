package net.foxopen.fox.module.serialiser.widgets.html;

import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.image.ObscureText;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.fieldset.fieldmgr.FieldMgr;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;
import net.foxopen.fox.thread.storage.TempResource;
import net.foxopen.fox.thread.storage.TempResourceGenerator;
import net.foxopen.fox.track.Track;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.OutputStream;


public class CaptchaWidgetBuilder extends WidgetBuilderHTMLSerialiser<EvaluatedNode> {
  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> INSTANCE = new CaptchaWidgetBuilder();

  public static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> getInstance() {
    return INSTANCE;
  }

  private CaptchaWidgetBuilder() {
  }

  @Override
  public void buildWidget(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNode pEvalNode) {
    //Avoid sending
    buildWidgetInternal(pSerialisationContext, pSerialiser, pEvalNode);
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNode pEvalNode) {
    Track.pushDebug("CaptchaWidget", pEvalNode.getDataItem().absolute());
    try {
      FieldMgr lFieldMgr = pEvalNode.getFieldMgr();

      final String lNodeValue = lFieldMgr.getSingleTextValue();

      if(lNodeValue.length() == 0) {
        throw new ExInternal("Captcha widget error: field " + pEvalNode.getIdentityInformation() + " must have content.");
      }

      //Hardcoded heights to legacy defaults
      final int lFieldHeight = 72;//(int)(Math.max(Integer.parseInt(pEvalNode.getFieldHeight())  * 12, 50));
      final int lFieldWidth = 200;//(int)(Math.max(Integer.parseInt(pEvalNode.getFieldWidth()) * 2.5, 100));

      //Register a TempResourceGenerator which will generate the captcha JIT on request
      TempResource<?> lBlobTempResource = pSerialisationContext.createTempResource(new CaptchaTempResourceGenerator(lFieldWidth, lFieldHeight, lNodeValue));

      //Generate the temp resource URI for the image
      String lImageURI = pSerialisationContext.getTempResourceURI(lBlobTempResource, "captcha.png");

      // TODO - This could be a data-uri, but IE7 doesn't support it
      pSerialiser.append("<img src=\"" + lImageURI + "\" alt=\"If you cannot see this image CAPTCHA please contact the system administrator for advice\"/>");
    }
    finally {
      Track.pop("CaptchaWidget");
    }
  }

  private static class CaptchaTempResourceGenerator
  implements TempResourceGenerator {
    private final int mFieldWidth;
    private final int mFieldHeight;
    private final String mNodeValue;

    public CaptchaTempResourceGenerator(int pFieldWidth, int pFieldHeight, String pNodeValue) {
      mFieldWidth = pFieldWidth;
      mFieldHeight = pFieldHeight;
      mNodeValue = pNodeValue;
    }

    @Override
    public void streamOutput(OutputStream pDestination)
    throws IOException {

      byte[] lImgData;
      try {
        lImgData = ObscureText.getObscureTextAsByteArray(mFieldWidth, mFieldHeight, mNodeValue);
      }
      catch (IOException e) {
        throw new ExInternal("Error converting image to byte array in getObscureTextAsByteArray" + e);
      }

      IOUtils.write(lImgData, pDestination);
    }

    @Override
    public String getContentType() {
      return "image/png";
    }

    @Override
    public long getCacheTimeMS() {
      return 0;
    }
  }
}
