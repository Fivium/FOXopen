package net.foxopen.fox.module.mapset;

import net.foxopen.fox.module.Template;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.mapset.MapSetDefinitionFactory.DefinitionBuilder;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.storage.CacheKey;

public class TemplateDefinition
extends MapSetDefinition {

  static class Builder
  extends DefinitionBuilder {

    private final Template mTemplate;

    Builder(Mod pModule, DOM pDefinitionElement)
    throws ExModule {

      //Determine target template name
      String lTemplateName = pDefinitionElement.getAttrOrNull("name");
      if(XFUtil.isNull(lTemplateName)) {
        throw new ExModule("name attribute cannot be null for a template based mapset");
      }

      //Resolve and validate the referenced template
      mTemplate = pModule.getTemplate(lTemplateName);
      if(mTemplate == null) {
        throw new ExModule("Could not locate a template named " + lTemplateName + " in module " + pModule.getName());
      }
    }

    @Override
    protected CacheKey getAutoCacheKeyOrNull() {
      return CacheKey.createStaticCacheKey();
    }

    @Override
    protected Long getDefaultTimeoutMinsOrNull() {
      return MapSetDefinitionFactory.NEVER_REFRESH_TIMEOUT_MINS;
    }

    @Override
    protected MapSetDefinition buildInternal(String pLocalName, CacheKey pCacheKey, XDoCommandList pXDo, long pRefreshTimeoutMins, Mod pModule)
    throws ExModule {
      return new TemplateDefinition(pLocalName, pCacheKey, pXDo, pRefreshTimeoutMins, pModule, mTemplate);
    }
  }

  private final Template mTemplate;

  private TemplateDefinition(String pLocalName, CacheKey pCacheKey, XDoCommandList pXDo, long pRefreshTimeoutMins, Mod pModule, Template pTemplate) {
    super(pLocalName, pCacheKey, pXDo, pRefreshTimeoutMins, pModule);
    mTemplate = pTemplate;
  }


  @Override
  protected DOM createMapSetDOM(ActionRequestContext pRequestContext, DOM pItemDOM, String pEvaluatedCacheKey, String pUniqueValue) {

    DOM lMapSetContainerDOM = createDefaultContainerDOM();

    //Copy template contents into the mapset DOM
    mTemplate.getTemplateElement().copyContentsTo(lMapSetContainerDOM);

    return lMapSetContainerDOM;
  }
}
