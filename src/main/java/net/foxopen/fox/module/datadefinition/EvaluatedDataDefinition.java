package net.foxopen.fox.module.datadefinition;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.datadefinition.datatransformer.ArrayTransformerRecordDeliverer;
import net.foxopen.fox.module.datadefinition.datatransformer.DataTransformerResultDeliverer;
import net.foxopen.fox.module.datadefinition.datatransformer.ObjectTransformerRecordDeliverer;
import net.foxopen.fox.thread.ActionRequestContext;
import org.json.simple.JSONArray;

import java.util.HashMap;
import java.util.Map;

public class EvaluatedDataDefinition {
  public static final String DEFAULT_INTERNAL_FOX_DATA_KEY = "DEFAULT_INTERNAL_FOX_DATA_KEY";

  private final String mDataDefinitionName;
  private final long mCreatedTimeMS;
  private final Map<String, JSONArray> mTransformedData = new HashMap<>();

  public EvaluatedDataDefinition(ActionRequestContext pRequestContext, DataDefinition pDataDefinition, ImplicatedDataDefinition pImplicatedDataDefinition) {
    mDataDefinitionName = pDataDefinition.getName();
    mCreatedTimeMS = System.currentTimeMillis();

    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    // Get match nodes
    DOMList lMatchList;
    try {
      lMatchList = lContextUElem.extendedXPathUL(pImplicatedDataDefinition.getMatchPath(), null);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to run query match XPath for Implicated Data Definition " + pDataDefinition.getName(), e);
    }

    // Get the column names
    String lDefinedSeriesName = pDataDefinition.getColumnMapping().get1SNoEx("fm:series");
    String lDefinedXColumnName = pDataDefinition.getColumnMapping().get1SNoEx("fm:x");
    if (XFUtil.isNull(lDefinedXColumnName)) {
      lDefinedXColumnName = "X";
    }
    String lDefinedYColumnName = pDataDefinition.getColumnMapping().get1SNoEx("fm:y");
    if (XFUtil.isNull(lDefinedYColumnName)) {
      lDefinedYColumnName = "Y";
    }

    // Run the query and transform the data
    UCon lUCon = pRequestContext.getContextUCon().getUCon("Evaluate Data Definition");
    try {
      for (DOM lMatchNode : lMatchList) {
        // Get an appropriate result deliverer
        DataTransformerResultDeliverer lDataTransformerResultDeliverer;
        switch (pDataDefinition.getDataTransformer()) {
          case OBJECT:
            lDataTransformerResultDeliverer = new ObjectTransformerRecordDeliverer(lDefinedSeriesName, lDefinedXColumnName, lDefinedYColumnName);
            break;
          case ARRAY:
            lDataTransformerResultDeliverer = new ArrayTransformerRecordDeliverer(lDefinedSeriesName, lDefinedXColumnName, lDefinedYColumnName);
            break;
          default:
            throw new ExInternal("No data transformer implemented for defined transformer type: " + pDataDefinition.getDataTransformer());
        }

        pDataDefinition.getQueryStatement().executeStatement(pRequestContext, lMatchNode, lUCon, lDataTransformerResultDeliverer);

        // Evaluate the foxDataKey attribute if there are multiple match nodes, otherwise default it
        String lFoxDataKey;
        if (lMatchList.size() >= 1 && !XFUtil.isNull(pImplicatedDataDefinition.getFoxDataKeyPath())) {
          try {
            lFoxDataKey = lContextUElem.extendedXPathString(lMatchNode, pImplicatedDataDefinition.getFoxDataKeyPath());
          }
          catch (ExActionFailed e) {
            throw new ExInternal("Failed to run query match XPath for Implicated Data Definition " + pDataDefinition.getName(), e);
          }
        }
        else if (lMatchList.size() == 1 && XFUtil.isNull(pImplicatedDataDefinition.getFoxDataKeyPath())) {
          lFoxDataKey = DEFAULT_INTERNAL_FOX_DATA_KEY;
        }
        else {
          throw new ExInternal("When the match path (" + pImplicatedDataDefinition.getMatchPath() + ") for an implicated data definition (" + pImplicatedDataDefinition.getDataDefinitionName() + ") resolves to more than 1 node you MUST specify a foxDataKey XPath which resolves to a unique string");
        }

        // Check there isn't any key-clash
        if (mTransformedData.containsKey(lFoxDataKey)) {
          throw new ExInternal("You MUST specify a foxDataKey XPath which resolves to a unique string, '" + lFoxDataKey + "' already evaluated");
        }

        mTransformedData.put(lFoxDataKey, lDataTransformerResultDeliverer.getTransformedData());
      }
    }
    catch (ExDB e) {
      throw new ExInternal("Error running Data Definition query", e);
    }
    finally {
      pRequestContext.getContextUCon().returnUCon(lUCon, "Evaluate Data Definition");
    }
  }

  public String getDataDefinitionName() {
    return mDataDefinitionName;
  }

  public Map<String, JSONArray> getTransformedData() {
    return mTransformedData;
  }

  public long getCreatedTimeMS() {
    return mCreatedTimeMS;
  }
}
