package net.foxopen.fox.entrypoint.engine;

import net.foxopen.fox.entrypoint.ws.WebService;
import net.foxopen.fox.entrypoint.ws.WebServiceCategory;

import java.util.Arrays;
import java.util.Collection;

public class EngineWebServiceCategory
implements WebServiceCategory {
  public static final String CATEGORY_NAME = "engine";

  @Override
  public String getName() {
    return CATEGORY_NAME;
  }

  @Override
  public Collection<? extends WebService> getAllWebServices() {
    return Arrays.asList(
      new AliveWebService(),
      new AppCacheWebService(),
      new ThreadWebService(),
      new MapSetWebService(),
      new SpatialWebService()
    );
  }
}
