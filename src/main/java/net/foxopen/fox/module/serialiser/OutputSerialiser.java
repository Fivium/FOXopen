package net.foxopen.fox.module.serialiser;

import net.foxopen.fox.module.OutputHint;
import net.foxopen.fox.module.evaluatedattributeresult.StringAttributeResult;
import net.foxopen.fox.module.fieldset.action.InternalActionContext;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;

import java.util.Map;


/**
 * Methods needed to serialise an evaluated parse tree to a writer
 */
public interface OutputSerialiser {


  /**
   * Get the appropriate widget builder from the serialiser
   *
   * @param pWidgetBuilderEnum
   * @return
   */
  public WidgetBuilder<? extends OutputSerialiser, ? extends EvaluatedNode> getWidgetBuilder(WidgetBuilderType pWidgetBuilderEnum);

  /**
   * Get the appropriate component builder from the serialiser
   *
   * @param pComponentEnum
   * @return
   */
  public ComponentBuilder<? extends OutputSerialiser, ? extends EvaluatedPresentationNode> getComponentBuilder(ComponentBuilderType pComponentEnum);

  /**
   * Create a temporary serialiser that extends the current serialiser implementation but implements TempSerialiser
   *
   * @return
   */
  public TempSerialiser getTempSerialiser();

  /**
   * Add hint features for a given Evaluated Node Info. In a HTML Serialiser this might be an image with a tooltip, in a
   * PDF serialiser it might be inline text and a CSV serialiser could do nothing.
   *
   * @param pHint
   * @param pTargetID
   * @param pAddIcon
   */
  public void addHint(OutputHint pHint, String pTargetID, boolean pAddIcon);

  public void addHint(OutputHint pHint);

  /**
   * Appends description text into the serialiser for the given node. This may not append anything if the node has no
   * description or if it does not think one should be displayed.
   * @param pEvaluatedNode
   */
  public void addDescription(EvaluatedNode pEvaluatedNode);

  /**
   * Gets the Javascript function call string for the given internal action.
   * @param pActionContext
   * @return
   */
  public String getInternalActionSubmitString(InternalActionContext pActionContext);

  /**
   * Gets the Javascript function call string for the given internal action.
   * @param pActionContext Internal action context.
   * @param pParamMap Additional parameters for the action call.
   * @return
   */
  public String getInternalActionSubmitString(InternalActionContext pActionContext, Map<String, String> pParamMap);

  /**
   * Output serialisers should define a function to sanitise strings for safe output. This is then used when getting
   * attributes, such as prompts/hints, to make sure they're safe for output.
   * The implementation should call pEvalNode.getStringAttributeResultOrNull() and test lAttributeResult.isEscapingRequired()
   * to find out when application developers have requested for no safely escaped strings. By default it should be safely
   * escaped.
   *
   * @param pStringAttributeResult Attribute result with a getString() method on it that might require escaping
   * @return Evaluated value of pNodeAttribute, safely escaped for output if required
   */
  public String getSafeStringAttribute(StringAttributeResult pStringAttributeResult);

  /**
   * Escape all the newline characters in pString to whatever the serialiser may need to reflect that newline.
   *
   * @param pString string to be escaped
   * @return copy pString with newlines replaced
   */
  public String escapeNewlines(String pString);

  /**
   * Add some debug information to the output
   *
   * @param pDebugInformation
   */
  public void addDebugInformation(String pDebugInformation);
}
