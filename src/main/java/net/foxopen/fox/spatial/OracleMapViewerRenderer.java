package net.foxopen.fox.spatial;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.io.IOUtil;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.track.Track;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

public class OracleMapViewerRenderer implements SpatialRenderer {
  private final String mRequestURI;

  protected OracleMapViewerRenderer(DOM pRendererConfig) {
    try {
      mRequestURI = pRendererConfig.get1S("url");
    }
    catch (ExCardinality e) {
      throw e.toUnexpected("Failed to configure OracleMapViewerRenderer");
    }
  }

  @Override
  public String getRenderXmlKey() {
    return "MAP_REQUEST";
  }

  /**
   * Post the render xml to a URL that should be pointing at MapViewer
   *
   * @param pRequestContext context
   * @param pRenderXML Render XML to post to MapViewer
   * @param pOutputStream OutputStream to write the resulting image to
   */
  @Override
  public void processRenderXML(RequestContext pRequestContext, DOM pRenderXML, OutputStream pOutputStream) {
    Track.pushInfo("SpatialRenderer", "Oracle MapViewer rendering started");
    Track.timerStart("SpatialRenderer");
    try {
      // Add in standard form post key/value pair emulation
      String lXMLRequest = "xml_request=" + URLEncoder.encode(pRenderXML.outputDocumentToString(false), "UTF-8");

      // Set up the POST request
      URL lURL = new URL(mRequestURI);
      URLConnection lURLConnection = lURL.openConnection();

      // Append headers
      lURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      lURLConnection.setRequestProperty("Content-Length", String.valueOf(lXMLRequest.length()));
      lURLConnection.setDoOutput(true);

      // Write out
      OutputStreamWriter lOutputStreamWriter = new OutputStreamWriter(lURLConnection.getOutputStream());
      lOutputStreamWriter.write(lXMLRequest);
      lOutputStreamWriter.flush();
      lOutputStreamWriter.close();

      // Get the response
      IOUtil.transfer(lURLConnection.getInputStream(), pOutputStream);
    }
    catch (MalformedURLException ex) {
      throw new ExInternal("Oracle MapViewer URL is not valid", ex);
    }
    catch (IOException ex) {
      throw new ExInternal("Failed to read image from Oracle MapViewer", ex);
    }
    finally {
      Track.timerPause("SpatialRenderer");
      Track.pop("SpatialRenderer");
    }
  }
}
