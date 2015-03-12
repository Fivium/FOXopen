package net.foxopen.fox.module.fieldset.transformer;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.util.StringFormatter;

import java.util.HashMap;
import java.util.Map;

public class FieldTransformerType {

  /**
   * Stores mapping of schema datatypes to XML date format masks
   */
  private static final Map<String, String> TYPE_TO_PARSE_FORMAT_MAP = new HashMap<>();

  /**
   * Stores mapping of schema datatypes to Oracle date format masks
   */
  private static final Map<String, String> TYPE_TO_DISPLAY_FORMAT_MAP = new HashMap<>();

  static {
    TYPE_TO_PARSE_FORMAT_MAP.put("xs:date", StringFormatter.XML_DATE_FORMAT_MASK);
    TYPE_TO_PARSE_FORMAT_MAP.put("xs:dateTime", StringFormatter.XML_DATETIME_FORMAT_MASK);
    TYPE_TO_PARSE_FORMAT_MAP.put("xs:time", StringFormatter.XML_TIME_FORMAT_MASK);

    //TODO PN these need to link up to the widget builder
    TYPE_TO_DISPLAY_FORMAT_MAP.put("xs:date", StringFormatter.ORA_DATE_FORMAT_MASK);
    TYPE_TO_DISPLAY_FORMAT_MAP.put("xs:dateTime", StringFormatter.ORA_DATETIME_NO_SECONDS_FORMAT_MASK);
    TYPE_TO_DISPLAY_FORMAT_MAP.put("xs:time", StringFormatter.ORA_TIME_FORMAT_MASK);
  }

  private FieldTransformerType() {}

  public static FieldTransformer getTransformerForNode(EvaluatedNodeInfo pEvaluatedNodeInfo) {

    String lDataType = "";
    if(pEvaluatedNodeInfo.getNodeInfo() != null) {
      lDataType = pEvaluatedNodeInfo.getNodeInfo().getDataType();
    }

    //Can be null
    String lInputMaskName = pEvaluatedNodeInfo.getStringAttribute(NodeAttribute.INPUT_MASK);

    //TODO PN node info datatype should be enum
    switch(lDataType) {
      case "xs:date":
      case "xs:dateTime":
      case "xs:time":
        return getDateFieldTransformer(pEvaluatedNodeInfo);
      case "xs:decimal":
        return new DecimalTransformer(lInputMaskName);
      case "xs:string":
      default:
        CaseOption lCaseOption = CaseOption.fromExternalString(pEvaluatedNodeInfo.getStringAttribute(NodeAttribute.CASE));
        if(CleanOption.fromExternalString(pEvaluatedNodeInfo.getStringAttribute(NodeAttribute.CLEAN)) == CleanOption.NO_TRIM) {
          return new NoTrimTextTransformer(lCaseOption, lInputMaskName);
        }
        else {
          return new TextTransformer(lCaseOption, lInputMaskName);
        }
    }
  }

  private static FieldTransformer getDateFieldTransformer(EvaluatedNodeInfo pEvaluatedNodeInfo) {

    String lDataType = pEvaluatedNodeInfo.getNodeInfo().getDataType();
    String lOutboundFormat = pEvaluatedNodeInfo.getStringAttribute(NodeAttribute.FORMAT_DATE);
    if(XFUtil.isNull(lOutboundFormat)) {
      lOutboundFormat = TYPE_TO_DISPLAY_FORMAT_MAP.get(lDataType);
    }

    return new DateTransformer(lOutboundFormat, TYPE_TO_PARSE_FORMAT_MAP.get(lDataType));
  }
}
