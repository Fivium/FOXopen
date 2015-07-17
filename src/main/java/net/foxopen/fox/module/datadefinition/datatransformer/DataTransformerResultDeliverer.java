package net.foxopen.fox.module.datadefinition.datatransformer;

import net.foxopen.fox.database.sql.QueryResultDeliverer;
import org.json.simple.JSONArray;

public interface DataTransformerResultDeliverer
  extends QueryResultDeliverer{
  public JSONArray getTransformedData();
}
