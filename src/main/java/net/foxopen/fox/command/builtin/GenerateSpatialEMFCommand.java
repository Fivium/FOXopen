package net.foxopen.fox.command.builtin;

import java.io.IOException;
import java.io.OutputStream;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.command.util.GeneratorDestination;
import net.foxopen.fox.command.util.GeneratorDestinationUtils;
import net.foxopen.fox.command.util.OutputStreamGenerator;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.renderer.Renderer;
import net.foxopen.fox.renderer.RendererEMF;
import net.foxopen.fox.spatial.SpatialEngine;
import net.foxopen.fox.spatial.SpatialRenderer;
import net.foxopen.fox.thread.ActionRequestContext;

public class GenerateSpatialEMFCommand extends BuiltInCommand {

  private final String mSpatialDefinition;
  private final String mSpatialInterface;
  private final String mSpatialCanvasCacheKey;

  private final GeneratorDestination mGeneratorDestination;

  public GenerateSpatialEMFCommand(DOM pCommandDOM) {
    super(pCommandDOM);

    mSpatialDefinition = pCommandDOM.getAttrOrNull("spatial-definition");
    mSpatialInterface = pCommandDOM.getAttrOrNull("spatial-interface");
    mSpatialCanvasCacheKey = pCommandDOM.getAttrOrNull("canvas-cache-key");

    String lFileExtension = ".emf";
    String lDefaultFileName = "generated-" + XFUtil.unique() + lFileExtension;

    mGeneratorDestination = GeneratorDestinationUtils.getDestinationFromGenerateCommandMarkup(pCommandDOM, lDefaultFileName, "application/emf");
  }

  @Override
  public boolean isCallTransition() {
    return false;
  }


  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    // Get ref to SpatialEngine
    SpatialEngine lSpatialEngine = null; //TODO PN pXThread.getTopApp().getSpatialEngine();
    if (lSpatialEngine == null) {
      throw new ExInternal("Cartographic widget could not be instantiated", new ExApp("Spatial engine not defined for application " + pRequestContext.getModuleApp().getMnemonicName()));
    }

    UCon lUCon = null; //TODO PN

    //Construct bootstrap DOM
    String lDefMnem = mSpatialDefinition;
    String lIntfMnem = mSpatialInterface;
    String lCacheKey = mSpatialCanvasCacheKey;

    // Enforce mandatory attributes for cartographic widget set-out
    if (XFUtil.isNull(lDefMnem)) {
      throw new ExInternal("Null value for spatial-definition found on schema element fm:generate");
    }
    else if (XFUtil.isNull(lIntfMnem)) {
      throw new ExInternal("Null value for spatial-interface found on schema element fm:generate");
    }
    else if (XFUtil.isNull(lCacheKey)) {
      throw new ExInternal("Null value for canvas-cache-key found on schema element fm:generate");
    }

    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    // Stringify definition / interface mnemonics and derive cache key by XPath
    try {
      lDefMnem = XFUtil.nvl(lContextUElem.extendedXPathString(lContextUElem.attachDOM(), lDefMnem), lDefMnem);
      lIntfMnem = XFUtil.nvl(lContextUElem.extendedXPathString(lContextUElem.attachDOM(), lIntfMnem), lIntfMnem);
      lCacheKey = XFUtil.nvl(lContextUElem.extendedXPathString(lContextUElem.attachDOM(), lCacheKey), lCacheKey);
    }
    catch (ExActionFailed ex) {
      throw new ExInternal("Could not process XPath", ex);
    }

    DOM lSpatialBootstrapDOM = DOM.createDocument("spatial-canvas-bootstrap");
    if (!XFUtil.isNull(lDefMnem) && !XFUtil.isNull(lIntfMnem)) {
      lSpatialBootstrapDOM.addElem("spatial-definition-mnem", lDefMnem);
      lSpatialBootstrapDOM.addElem("spatial-interface-mnem", lIntfMnem);
      lSpatialBootstrapDOM.addElem("canvas-cache-key", lCacheKey);
      lSpatialBootstrapDOM.addElem("data").addElem("PROJECT_CACHE_KEY", lCacheKey);
    }
    else {
      throw new ExInternal("Evaluated values of ns:spatial-definition and ns:spatial-interface may not be null");
    }

    //BootstrapSpatialCanvas from the bootstrap DOM
    lSpatialBootstrapDOM = lSpatialEngine.bootstrapSpatialCanvas(lSpatialBootstrapDOM, pRequestContext.getCurrentCallId(), pRequestContext.getAuthenticationContext().getAuthenticatedUser().getAccountID(), lUCon);

    //Start rendering with canvas id, width and height
    String lXmlRequestBody;
    String lCanvasId;
    Integer lWidth;
    Integer lHeight;
    String lStatement = "DECLARE\n" + "  l_render_xml XMLTYPE;\n" + "BEGIN\n" + "  l_render_xml := spatialmgr.spm_fox.generate_render_xml(\n" + "    p_sc_id      => :1\n" + "  , p_width_px   => :2\n" + "  , p_height_px  => :3\n" + "  , p_datasource => :4\n" + "  );\n" + "  :5 := l_render_xml.getClobVal();\n" + "END;";

    //      try {
    //        // Get canvas id
    //        lCanvasId = lSpatialBootstrapDOM.get1S("/*/canvas-id");
    //        lWidth = new Integer(lSpatialBootstrapDOM.get1S("/*/canvas-width").replaceAll("[^0-9]", ""));
    //        lHeight = new Integer(lSpatialBootstrapDOM.get1S("/*/canvas-height").replaceAll("[^0-9]", ""));
    //
    //        // API params
    //        Object lParams[] = { lCanvasId, lWidth, lHeight, pXThread.getTopApp().getConnectKey() // TODO AT [ConnectKey]
    //          , CLOB.class
    //        };
    //
    //        lUCon.executeCall(lStatement, lParams, new char[] { 'I', 'I', 'I', 'I', 'O' });
    //
    //        lXmlRequestBody = SQLTypeConverter.clobToString((CLOB) lParams[4]); //TODO PN UCON read string directly from result
    //      }
    //      catch (ExDB ex) {
    //        throw new ExInternal("Query failed in Spatial Renderer", ex);
    //      }
    //      catch (ExTooMany ex) {
    //        throw new ExInternal("Cardinality error resulting from spatial rendering query", ex);
    //      }
    //      catch (ExTooFew ex) {
    //        throw new ExInternal("Cardinality error resulting from spatial rendering query", ex);
    //      }
    //      catch (ExServiceUnavailable ex) {
    //        throw new ExInternal("Service unavailable in spatial rendering query", ex);
    //      }

    lXmlRequestBody = "TODO";

    DOM lRequestDOM = DOM.createDocumentFromXMLString(lXmlRequestBody);
    int lEMFHeight, lEMFWidth;
    lEMFHeight = Integer.parseInt(lRequestDOM.get1SNoEx("/INTERNAL_MAP_REQUEST/HEIGHT"));
    lEMFWidth = Integer.parseInt(lRequestDOM.get1SNoEx("/INTERNAL_MAP_REQUEST/WIDTH"));

    final Renderer lEMF = new RendererEMF(lEMFWidth, lEMFHeight, 72, null);

    SpatialRenderer.internalRender(lRequestDOM, lUCon, lEMF);

    //Store generate file
    mGeneratorDestination.generateToOutputStream(pRequestContext, new OutputStreamGenerator() {
      @Override
      public void writeOutput(OutputStream pOutputStream) throws IOException {
        pOutputStream.write(lEMF.generate());
      }
    });

    return XDoControlFlowContinue.instance();
  }
}
