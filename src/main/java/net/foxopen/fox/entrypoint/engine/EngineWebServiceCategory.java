package net.foxopen.fox.entrypoint.engine;

import net.foxopen.fox.entrypoint.ws.WebService;
import net.foxopen.fox.entrypoint.ws.WebServiceCategory;

import java.util.Arrays;
import java.util.Collection;

public class EngineWebServiceCategory
implements WebServiceCategory {

  @Override
  public String getName() {
    return "engine";
  }

  @Override
  public Collection<? extends WebService> getAllWebServices() {
    return Arrays.asList(
      new AliveWebService(),
      new ThreadWebService()
    );
  }
}
