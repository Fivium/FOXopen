package net.foxopen.fox.dbinterface.deliverer;

import net.foxopen.fox.database.sql.out.JDBCResultAdaptor;
import net.foxopen.fox.database.sql.out.SQLTypeConverter;
import net.foxopen.fox.dbinterface.DOMDataType;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.module.datanode.NodeInfo;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackFlag;

import java.sql.SQLException;
import java.sql.Types;


/**
 * Utility methods encapsulating functionality shared by all DB Interface deliverers.
 */
public final class DelivererUtils {
  private DelivererUtils() {}

  /**
   * Calculates the DOMDataType for an output parameter without one explicitly specified, using the following logic:
   * <ol>
   * <li>If the destination path has a corresponding NodeInfo defined in the schema, use the datatype from that:
   *   <ul>
   *   <li>If the node is a complex type, assume the column is XML.</li>
   *   <li>If the node is a supported simple type (string, datetime, etc), assume that.</li>
   *   <li>Otherwise assume string.</li>
   *   </ul>
   * <li>Or, if there is no NodeInfo available, use the column's SQLType:
   *   <ul>
   *   <li>For XMLTypes assume XML.</li>
   *   <li>Otherwise assume string.</li>
   *   </ul>
   * </li>
   * @param pRequestContext Current RequestContext.
   * @param pParamSQLType SQL type of the selected column (as in java.sql.types).
   * @param pDestinationRelativeDOM Relative DOM of the destination for XPath evaluation (i.e. row container/match node)
   * @param pDestinationRelativePath Path to the result destination, relative to the relative DOM.
   * @param pParamName Name of column/out bind for debug error messages.
   * @return
   */
  private static DOMDataType establishDestinationDatatype(ActionRequestContext pRequestContext, int pParamSQLType,
                                                          DOM pDestinationRelativeDOM, String pDestinationRelativePath,
                                                          String pParamName) {

    //Work out the absolute path for the potential new element - this will be used to work out the NodeInfo for the target
    String lTargetAbsolutePath = "";
    try {
      lTargetAbsolutePath = pRequestContext.getContextUElem().getAbsolutePathForCreateableXPath(pDestinationRelativeDOM, pDestinationRelativePath);
    }
    catch (ExActionFailed | ExTooMany e) { /* Ignore errors here - report them later on when the column is created */}

    //Attempt to find the NodeInfo for the target column
    NodeInfo lColNodeInfo = pRequestContext.getCurrentModule().getNodeInfo(lTargetAbsolutePath);

    DOMDataType lDestinationDatatype;
    if (lColNodeInfo == null) {
      //The target element is not defined in the schema - use the SQL column type
      //EXPERIMENTAL: default to DOM if the selected column is XML
      if(pParamSQLType == Types.SQLXML) {
        Track.info("ImplicitXMLTypeConversion", "Assuming column " + pParamName + " should be bound out as XML because SQL type was XML", TrackFlag.REGRESSION_CHANGE);
        lDestinationDatatype = DOMDataType.DOM;
      }
      else {
        //If not SQLXML, assume string (dates will be converted using default formatter later)
        lDestinationDatatype = DOMDataType.STRING;
      }
    }
    else if(!lColNodeInfo.getIsItem()) {
      //EXPERIMENTAL: default to DOM if the selected column is XML
      Track.info("ImplicitXMLTypeConversion", "Assuming column " + pParamName + " should be bound out as XML because schema is marked up as complex type", TrackFlag.REGRESSION_CHANGE);
      lDestinationDatatype = DOMDataType.DOM;
    }
    else {
      //Convert xs type (xs:date, xs:datetime, ...) from nodeinfo to DOMDataType
      lDestinationDatatype = DOMDataType.fromExternalString(lColNodeInfo.getDataType());
      //If the NodeInfo datatype is not known the above method will return null.
      //Unsupported datatypes (xs:integer etc) should be converted to String
      lDestinationDatatype = (lDestinationDatatype == null ? DOMDataType.STRING : lDestinationDatatype);
    }

    return lDestinationDatatype;
  }

  /**
   * Shared method for writing an out parameter or column from a query or API result into a DOM. The logic for establishing
   * a default datadom-type if one is not specified, and converting the SQL column value into an appropriate Java object,
   * is encapsulated in this method.
   * @param pRequestContext Current RequestContext.
   * @param pResultAdaptor Wrapper for the JDBC ResultSet or executed CallableStatement.
   * @param pParamIndex 1-based index of the parameter to be read from the result adaptor.
   * @param pParamSQLType SQL type of the selected column (as in java.sql.types).
   * @param pDestinationRelativeDOM Relative DOM of the destination for XPath evaluation (i.e. row container/match node)
   * @param pDestinationRelativePath Path to the result destination, relative to the relative DOM. This will be created
   * if it does not exist (subject to the caveat described below for pCreateTargetForDOMWhenNull).
   * @param pOptionalDestinationDataType The fm:into/fm:using direction="out" datadom-type for the parameter, if specified.
   * Can be null if no definition was provided - in which case, the default datatype logic is invoked. Consumers calling
   * this method for multiple iterations should cache the returned value and pass it as this parameter in subsequent
   * calls to avoid re-invoking potentially expensive default calculation.
   * @param pParamName Name of column/out bind for debug error messages.
   * @param pCreateTargetForDOMWhenNull If true, when a DOM is selected its destination element will always be created,
   * even if the DOM is null. Legacy behavior was to NOT create a container for null DOMs in API calls, but do create one
   * for query columns.
   * @param pPurgeSelectedDOMs If true, the contents of a targeted DOM node is removed before it is populated from the result
   * set (datadom-type="dom" only).
   * @return The DOMDataType actually used to do the conversion.
   * @throws SQLException If the result set cannot be read.
   */
  public static DOMDataType convertResultAndPopulateDOM (ActionRequestContext pRequestContext, JDBCResultAdaptor pResultAdaptor,
                                                  int pParamIndex, int pParamSQLType, DOM pDestinationRelativeDOM,
                                                  String pDestinationRelativePath, DOMDataType pOptionalDestinationDataType,
                                                  String pParamName, boolean pCreateTargetForDOMWhenNull, boolean pPurgeSelectedDOMs)
  throws SQLException {

    //If destination type is not provided, work out the default
    DOMDataType lDestinationDataType = null;
    if(pOptionalDestinationDataType == null) {
      lDestinationDataType = establishDestinationDatatype(pRequestContext, pParamSQLType, pDestinationRelativeDOM, pDestinationRelativePath, pParamName);
    }
    else {
      lDestinationDataType = pOptionalDestinationDataType;
    }

    DOM lTargetDOM = null;
    try {
      if (lDestinationDataType == DOMDataType.DOM) {
        //Get the column value as a DOM if we're writing a DOM, then copy its contents to the destination path (strips containing element)
        DOM lColDOMValue = SQLTypeConverter.getValueAsDOM(pResultAdaptor, pParamIndex, pParamSQLType);

        //Get or create a node to contain the current column's value (only if the DOM is not null or we're being forced to)
        if(lColDOMValue != null || pCreateTargetForDOMWhenNull) {
          lTargetDOM = pRequestContext.getContextUElem().extendedXPath1E(pDestinationRelativeDOM, pDestinationRelativePath, true);
        }

        if(pPurgeSelectedDOMs && lTargetDOM != null) {
          lTargetDOM.removeAllChildren();
        }

        //Might be null if the column was null
        if(lColDOMValue != null) {
          lColDOMValue.copyContentsTo(lTargetDOM);
        }
      }
      else {
        //Always create a target node for non-DOM output
        lTargetDOM = pRequestContext.getContextUElem().extendedXPath1E(pDestinationRelativeDOM, pDestinationRelativePath, true);

        //Get the value of the column as a String
        String lColStringValue;
        if (pParamSQLType == Types.TIMESTAMP) {
          //This is a date - convert to a date string based on format mask configured on DOMDataType
          //Note: currently the default mask for a xs:string is the xs:dateTime mask. If this method is passed null
          //as a format mask instead, it will do a cleverer conversion based on the existence of a time component.
          //TODO switch date to string logic format mask logic here if deemed appropriate.
          lColStringValue = SQLTypeConverter.getValueAsDateString(pResultAdaptor, pParamIndex, lDestinationDataType.getDateFormatMask());
        }
        else {
          //For all other types get the string value
          lColStringValue = SQLTypeConverter.getValueAsString(pResultAdaptor, pParamIndex, pParamSQLType);
        }

        //Set the columm's text value
        lTargetDOM.setText(lColStringValue);
      }

      return lDestinationDataType;
    }
    catch (ExActionFailed | ExCardinality e) {
      throw new ExInternal("Output column " + pParamName + " could not be added to DOM destination using path '" + pDestinationRelativePath + "'", e);
    }
  }
}
