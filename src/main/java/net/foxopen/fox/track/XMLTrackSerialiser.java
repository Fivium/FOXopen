package net.foxopen.fox.track;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.FoxResponseCHARStream;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.ActuateReadOnly;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class XMLTrackSerialiser
implements TrackSerialiser {

  private static final XMLTrackSerialiser INSTANCE = new XMLTrackSerialiser();

  private static final int INDENT_SPACES = 2;

  public static final XMLTrackSerialiser instance() {
    return INSTANCE;
  }

  private XMLTrackSerialiser() {
  }

  /**
   * Open an element of pElementName, only allowing basic element name characters through
   *
   * @param pElementName
   * @return
   */
  private static String openElement(String pElementName) {
    return "<" + pElementName.replaceAll("[^a-zA-Z0-9-_:.]", "");
  }

  private static String endOpenTag() {
    return ">\n";
  }

  private static String selfCloseOpenTag() {
    return "/>\n";
  }

  private static String addAttribute(String pAttrName, String pValue) {
    return " " + pAttrName + "=\"" + pValue + "\"";
  }

  private static String closeElement(String pElementName) {
    return "</" + pElementName.replaceAll("[^a-zA-Z0-9-_:.]", "") + ">\n";
  }

  public void serialiseToWriter(TrackLogger pTrackLogger, Writer pWriter, boolean pPrintTimerDetail)
  throws IOException {

    pWriter.append(ActuateReadOnly.XML_DECLARATION_UTF_8);
    TrackEntry lTopEntry = pTrackLogger.getRootEntry();

    serialiseInternal(lTopEntry, pTrackLogger, true, pPrintTimerDetail, pWriter, 0);
  }


  private void serialiseInternal(TrackEntry pEntry, TrackLogger pTrackLogger, boolean pTopLevel, boolean pPrintTimerDetail, Writer pWriter, int pIndentLevel)
  throws IOException {

    if(pEntry.isVisible()) {
      for(int i=0; i < pIndentLevel * INDENT_SPACES; i++) {
        pWriter.append(" ");
      }

      pWriter.append(openElement(pEntry.getSubject()));

      pWriter.append(addAttribute("IN", String.valueOf(pEntry.getInTime() - pTrackLogger.getStartTimeMS())));
      pWriter.append(addAttribute("MS", String.valueOf(pEntry.getOutTime() - pEntry.getInTime())));
      pWriter.append(addAttribute("SEV", pEntry.getSeverity().toString()));

      if(pTopLevel) {
        pWriter.append(addAttribute("xmlns:fm", Mod.FOX_MODULE_URI));
      }

      List<TrackEntry> lChildEntries = pEntry.getChildEntryList();
      List<TrackEntry> lChildNonAttributeEntries = new ArrayList<>();

      //Find and serialise attributes; copy non-attributes aside for serialising below

      //IMPORTANT: use a for loop instead of an iterator to avoid mutation problems when serialising hot tracks.
      //This should be "safe" because we know track only ever adds child elements. Hot track may not see the "latest" track
      //but it'll see enough.
      for(int i=0; i < lChildEntries.size(); i++) {
        TrackEntry lChild = lChildEntries.get(i);
        if(lChild.getType() == TrackEntryType.ATTRIBUTE) {
          pWriter.append(addAttribute(lChild.getSubject(), StringEscapeUtils.escapeXml(lChild.getInfo())));
        }
        else {
          lChildNonAttributeEntries.add(lChild);
        }
      }

      boolean lTagClosed = false;
      if (!XFUtil.isNull(pEntry.getInfo())) {
        if (pEntry.getType() ==  TrackEntryType.TEXT || pEntry.getType() ==  TrackEntryType.XML) {
          pWriter.append(endOpenTag());
          if(pEntry.getType() ==  TrackEntryType.TEXT) {
            pWriter.append(StringEscapeUtils.escapeXml(pEntry.getInfo()));
          }
          else {
            //Don't escape XML
            pWriter.append(pEntry.getInfo());
          }

          lTagClosed = true;
        }
        else {
          //For DEFAULT entry types
          pWriter.append(addAttribute("DATA", StringEscapeUtils.escapeXml(pEntry.getInfo())));
        }
      }

      if(lChildNonAttributeEntries.size() > 0 && !lTagClosed) {
        pWriter.append(endOpenTag());
      }
      else if(!lTagClosed) {
        pWriter.append(selfCloseOpenTag());
        return;
      }

      //Print timer/counter info for the first iteration
      if(pTopLevel) {
        printTimerInfo(pTrackLogger, pWriter, pPrintTimerDetail);
        printCounterInfo(pTrackLogger, pWriter);
      }

      int lNewIndentLevel = pIndentLevel + 1;
      for(TrackEntry lChild : lChildNonAttributeEntries) {
        serialiseInternal(lChild, pTrackLogger, false, false, pWriter, lNewIndentLevel);
      }

      for(int i=0; i < pIndentLevel * INDENT_SPACES; i++) {
        pWriter.append(" ");
      }
      pWriter.append(closeElement(pEntry.getSubject()));
    }
  }

  private void printTimerInfo(TrackLogger pTrackLogger, Writer pWriter, boolean pPrintTimerDetail)
  throws IOException {

    pWriter.append(openElement("TimerInfo"));

    Collection<String> lTimerNames = pTrackLogger.getAllTimerNames();

    if(lTimerNames.size() == 0) {
      pWriter.append(selfCloseOpenTag());
    }
    else {
      pWriter.append(endOpenTag());
      for(String lTimerName : lTimerNames) {

        long lTimerTime = pTrackLogger.getTimerValue(lTimerName);
        List<TimerEntry> lTimerEntries = pTrackLogger.getTimerEntries(lTimerName);

        pWriter.append(openElement("Timer"));
        pWriter.append(addAttribute("NAME", lTimerName));
        pWriter.append(addAttribute("MS", Long.toString(lTimerTime)));
        pWriter.append(addAttribute("ENTRIES", Integer.toString(lTimerEntries.size())));

        //Print split times if required
        if(!pPrintTimerDetail) {
          pWriter.append(selfCloseOpenTag());
        }
        else {
          pWriter.append(endOpenTag());
          for(TimerEntry lEntry : lTimerEntries) {
            pWriter.append(openElement("Split"));
            pWriter.append(addAttribute("IN", Long.toString(lEntry.mStartTime - pTrackLogger.getStartTimeMS())));
            pWriter.append(addAttribute("OUT", Long.toString(lEntry.mEndTime - pTrackLogger.getStartTimeMS())));
            pWriter.append(selfCloseOpenTag());
          }
          pWriter.append(closeElement("Timer"));
        }
      }

      pWriter.append(closeElement("TimerInfo"));
    }
  }

  private void printCounterInfo(TrackLogger pTrackLogger, Writer pWriter)
  throws IOException {
    pWriter.append(openElement("CounterInfo"));

    Collection<String> lCounterNames = pTrackLogger.getAllCounterNames();

    if(lCounterNames.size() == 0) {
      pWriter.append(selfCloseOpenTag());
    }
    else {
      pWriter.append(endOpenTag());
      for(String lCounterName : lCounterNames) {
        pWriter.append(openElement("Counter"));
        pWriter.append(addAttribute("NAME", lCounterName));
        pWriter.append(addAttribute("VALUE", Integer.toString(pTrackLogger.getCounterValue(lCounterName))));
        pWriter.append(selfCloseOpenTag());
      }
      pWriter.append(closeElement("CounterInfo"));
    }
  }


  @Override
  public FoxResponse createFoxResponse(TrackLogger pTrackLogger, FoxRequest pFoxRequest, boolean pPrintTimerDetail) {
    FoxResponseCHARStream lFoxResponse = new FoxResponseCHARStream("text/xml", pFoxRequest, 0L);
    try {
      serialiseToWriter(pTrackLogger, lFoxResponse.getWriter(), pPrintTimerDetail);
    }
    catch (IOException e) {
      throw new ExInternal("Track XML serialisation failed", e);
    }
    return lFoxResponse;
  }
}
