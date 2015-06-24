package net.foxopen.fox.thread.devtoolbar;

import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilder;
import net.foxopen.fox.thread.RequestContext;

import java.util.Collection;
import java.util.Map;


/**
 * Provides data for consumption by the developer toolbar. Debug flags can be set on this object - these are cached across
 * page churns and should be stored seperately from any associated XThread. Consumers may check if a flag is set and branch
 * behaviour accordingly.
 */
public interface DevToolbarContext {

  /**
   * Flags which can be set on the dev toolbar to enable certain debug features.
   */
  public enum Flag {
    NO_CACHE(false, "Disable Thread Caching"),
    DBMS_OUTPUT(false, "Enable DBMS_OUTPUT"),
    SEMANTIC_FIELD_IDS(false, "Semantic Field IDs"),
    TRACK_UNATTACHED_LABEL(false, "Unattached label tracking"),
    HTML_GEN_DEBUG(false, "HTML Generator Debug");

    private final boolean mDefaultValue;
    private final String mDisplayKey;

    private Flag(boolean pDefaultValue, String pDisplayKey) {
      mDefaultValue = pDefaultValue;
      mDisplayKey = pDisplayKey;
    }

    public boolean getDefaultValue() {
      return mDefaultValue;
    }

    public String getDisplayKey(){
      return mDisplayKey;
    }
  }

  /**
   * Retrieves the debug page for the given type.
   * @param pPageType Type of debug page.
   * @return
   */
  public FoxResponse getDebugPage(RequestContext pRequestContext, DebugPage pPageType);

  public String getXPathResult(RequestContext pRequestContext);

  public DOM getDebugDOM(RequestContext pRequestContext, String pDOMName);

  /**
   * Gets all the document level context labels known to this DevToolbarContext.
   * @return
   */
  public Collection<String> getDocumentContextLabels();

  /**
   * Gets a map of contextual (i.e. state level) labels to their corresponding absolute paths.
   * @return
   */
  public Map<String, String> getContextLabelToPathMap();

  /**
   * Tests if a dev flag is currently on for this context.
   * @param pFlag Flag to test.
   * @return
   */
  public boolean isFlagOn(Flag pFlag);

  /**
   * Sets the given dev flag to the given value.
   * @param pFlag Flag to set.
   * @param pValue New value for flag.
   */
  public void setFlag(Flag pFlag, boolean pValue);


  public void setTrackedContextLabel(String pLabelName);

  public String getTrackedContextLabelOrNull();

  public String getEntryPointURI(RequestURIBuilder pRequestURIBuilder);

  public Collection<String> getXPathVariableNames();

}
