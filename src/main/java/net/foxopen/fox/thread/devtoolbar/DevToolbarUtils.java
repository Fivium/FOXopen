package net.foxopen.fox.thread.devtoolbar;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.FoxResponseCHAR;
import net.foxopen.fox.XFUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Static utility methods for the dev toolbar.
 */
public class DevToolbarUtils {

  public static final String DEV_FLAG_FORM_NAME = "devflag";
  public static final String TRACK_UNATTACHED_LABEL_NAME = "TRACK_UNATTACHED_LABEL_NAME";

  private DevToolbarUtils() {}

  /**
   * Sets flags on the given DevToolbarContext according to the contents of the posted form.
   * @param pFoxRequest HTTP request containing posted form values corresponding to dev toolbar flags.
   * @param pDevToolbarContext Context to update.
   * @return XML response, possibly containing an error message if the request failed.
   */
  public static FoxResponse applyPostedForm(FoxRequest pFoxRequest, DevToolbarContext pDevToolbarContext) {

    try {
      Map<String, String[]> lPostedFormValuesMap = pFoxRequest.getHttpRequest().getParameterMap();

      //Process special dev toolbar actions
      String[] lDevToolbarFlags = lPostedFormValuesMap.get(DEV_FLAG_FORM_NAME);
      Set <String> lFlagNames = Collections.emptySet();
      if(lDevToolbarFlags != null) {
        lFlagNames = new HashSet<>(Arrays.asList(lDevToolbarFlags));
      }

      for(DevToolbarContext.Flag lFlag : DevToolbarContext.Flag.values()) {
        pDevToolbarContext.setFlag(lFlag, lFlagNames.contains(lFlag.toString()));
      }

      String lTrackLabelName = lPostedFormValuesMap.get(TRACK_UNATTACHED_LABEL_NAME)[0];
      if(!XFUtil.isNull(lTrackLabelName)) {
        pDevToolbarContext.setTrackedContextLabel(lTrackLabelName);
      }

      return new FoxResponseCHAR("text/xml", new StringBuffer("<ok/>"), 0L);
    }
    catch (Throwable th) {
      return new FoxResponseCHAR("text/xml", new StringBuffer("<error>Failed: " + th.getMessage() + "</error>"), 0L);
    }
  }
}
