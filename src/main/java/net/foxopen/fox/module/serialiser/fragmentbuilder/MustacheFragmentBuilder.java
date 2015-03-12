package net.foxopen.fox.module.serialiser.fragmentbuilder;


import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import java.io.Writer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.track.Track;


public class MustacheFragmentBuilder {
  private static final MustacheFactory MUSTACHE_FACTORY = new DefaultMustacheFactory("net/foxopen/fox/module/serialiser/templates/");
  private static final Map<String, Mustache> TEMPLATE_CACHE = new ConcurrentHashMap<>();

  public static Mustache getTemplate(String pTemplateName) {
    Mustache lCachedTemplate = TEMPLATE_CACHE.get(pTemplateName);
    if (lCachedTemplate != null) {
      return lCachedTemplate;
    }
    else {
      try {
        Track.debug("MustacheCompile", "Compiling mustache template: " + pTemplateName);
        lCachedTemplate = MUSTACHE_FACTORY.compile(pTemplateName);
        TEMPLATE_CACHE.put(pTemplateName, lCachedTemplate);
        return lCachedTemplate;
      }
      catch (Throwable ex){
        throw new ExInternal("Fragment builder failed to find template: " + pTemplateName, ex);
      }
    }
  }

  public static void applyMapToTemplate(Mustache pTemplate, Map<String, Object> pMap, Writer pWriterOut) {
    pTemplate.execute(pWriterOut, pMap);
  }

  public static void applyMapToTemplate(String pTemplateName, Map<String, Object> pMap, Writer pWriterOut) {
    MustacheFragmentBuilder.getTemplate(pTemplateName).execute(pWriterOut, pMap);
  }
}
