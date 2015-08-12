package net.foxopen.fox.module.datanode;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.auth.AuthenticatedUser;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.GenericAttributesEvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.GenericAttributesPresentationNode;
import net.foxopen.fox.spatial.SpatialEngine;

import java.util.Optional;

/**
 * This is an Evaluated Node Info Item for items with widget set to cartographic
 * It bootstraps the spatial canvas and stores the information for the widget builders to use later
 */
public class EvaluatedNodeInfoCartographicItem extends EvaluatedNodeInfoItem {
  private final String mCanvasID;
  private final String mCanvasWidth;
  private final String mCanvasHeight;

  protected EvaluatedNodeInfoCartographicItem(EvaluatedNode pParent, GenericAttributesEvaluatedPresentationNode<? extends GenericAttributesPresentationNode> pEvaluatedPresentationNode, NodeEvaluationContext pNodeEvaluationContext, NodeVisibility pNodeVisibility, NodeInfo pNodeInfo) {
    super(pParent, pEvaluatedPresentationNode, pNodeEvaluationContext, pNodeVisibility, pNodeInfo);

    // Get ref to SpatialEngine
    SpatialEngine lSpatialEngine = pNodeEvaluationContext.getEvaluatedParseTree().getApp().getSpatialEngineOrNull();
    if (lSpatialEngine == null) {
      throw new ExInternal(
        "Cartographic widget could not be instantiated"
        , new ExApp("Spatial engine not defined for application " + pNodeEvaluationContext.getEvaluatedParseTree().getApp().getMnemonicName())
      );
    }

    String lSpatialDefinition = getStringAttribute(NodeAttribute.SPATIAL_DEFINITION);
    String lSpatialInterface = getStringAttribute(NodeAttribute.SPATIAL_INTERFACE);
    String lCanvasCacheKey = getStringAttribute(NodeAttribute.CANVAS_CACHE_KEY);

    // Enforce mandatory attributes for cartographic widget set-out
    if (XFUtil.isNull(lSpatialDefinition)) {
      throw new ExInternal("Null value for ns:spatial-definition found on schema element " + getIdentityInformation());
    }
    else if (XFUtil.isNull(lSpatialInterface)) {
      throw new ExInternal("Null value for ns:spatial-interface found on schema element " + getIdentityInformation());
    }
    else if (XFUtil.isNull(lCanvasCacheKey)) {
      throw new ExInternal("Null value for ns:canvas-cache-key found on schema element " + getIdentityInformation());
    }

    DOM lSpatialBootstrapDOM = DOM.createDocument("spatial-canvas-bootstrap");
    lSpatialBootstrapDOM.addElem("spatial-definition-mnem", lSpatialDefinition);
    lSpatialBootstrapDOM.addElem("spatial-interface-mnem", lSpatialInterface);
    lSpatialBootstrapDOM.addElem("canvas-cache-key", lCanvasCacheKey);
    DOM lPrimaryData = lSpatialBootstrapDOM.addElem("data");
    getDataItem().copyContentsTo(lPrimaryData);

    // Bootstrap widget from data
    lSpatialBootstrapDOM = lSpatialEngine.bootstrapSpatialCanvas(
      pNodeEvaluationContext.getEvaluatedParseTree().getRequestContext()
      , lSpatialBootstrapDOM
      , pNodeEvaluationContext.getEvaluatedParseTree().getThreadInfoProvider().getCurrentCallId()
      , pNodeEvaluationContext.getEvaluatedParseTree().getAuthenticatedUser().map(AuthenticatedUser::getAccountID).flatMap(Optional::ofNullable).orElse("0")
    );

    try {
      mCanvasID = lSpatialBootstrapDOM.get1S("canvas-id");
      mCanvasWidth = lSpatialBootstrapDOM.get1S("canvas-width");
      mCanvasHeight = lSpatialBootstrapDOM.get1S("canvas-height");
    }
    catch (ExCardinality ex) {
      throw new ExInternal("Failed to obtain canvas-width or canvas-height, ensure definition contains DEFAULT_WIDTH and DEFAULT_HEIGHT", ex);
    }
  }

  public String getCanvasID() {
    return mCanvasID;
  }

  public String getCanvasWidth() {
    return mCanvasWidth;
  }

  public String getCanvasHeight() {
    return mCanvasHeight;
  }
}
