package net.foxopen.fox.command.builtin;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.command.util.DownloadGeneratorDestination;
import net.foxopen.fox.database.sql.ExecutableQuery;
import net.foxopen.fox.dbinterface.InterfaceQuery;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.download.DownloadManager;
import net.foxopen.fox.download.DownloadParcel;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.track.Track;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;

public class ShowPopupQueryCommand
extends BuiltInCommand {

  /** The resource file name to show on the client. */
  private final String mFileName;

  /** Zip archive attributes. */
  private final String mZipArchiveName;
  private final String mZipCompressionLevel;

  /** Database query attributes. */
  private final String mDbInterface;
  private final String mDbQuery;
  private final String mDbMatch;

  /** Info about where the download is going. */
  private final DownloadGeneratorDestination mDownloadDestination;

  ShowPopupQueryCommand(Mod pModule, DOM pCommandDOM) {
    super(pCommandDOM);

    mDbInterface = pCommandDOM.getAttrOrNull("db-interface");
    mDbQuery = pCommandDOM.getAttrOrNull("db-query");
    mDbMatch = XFUtil.nvl(pCommandDOM.getAttrOrNull("db-match"), ".");

    mZipArchiveName = pCommandDOM.getAttrOrNull("zip-archive-name");
    mZipCompressionLevel = pCommandDOM.getAttrOrNull("zip-compression-level");

    mFileName = XFUtil.nvl(pCommandDOM.getAttrOrNull("file-name"), "file");

    mDownloadDestination = new DownloadGeneratorDestination(mFileName, "",  XFUtil.nvl(pCommandDOM.getAttrOrNull("serve-as-attachment"), "false()"),
                                                            XFUtil.nvl(pCommandDOM.getAttrOrNull("serve-as-response"), "false()"), "00:00");

    if (XFUtil.isNull(mDbInterface) ^ XFUtil.isNull(mDbQuery)) {
      throw new ExInternal("The " + getName() + " command has been used incorrectly in module \"" + pModule.getName() + " - \"db-interface\" has beeen defined without \"db-query\" or vice versa.");
    }
    else if (mZipArchiveName != null && mDbInterface == null) {
      throw new ExInternal("The " + getName() + " command has been used incorrectly in module \"" + pModule.getName() + " - \"zipArchiveName\" has beeen defined without \"db-interface\". Zip file generation currently only works with a db query.");
    }
  }

  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    //Retrieve the query
    InterfaceQuery lQuery = pRequestContext.getCurrentModule().getDatabaseInterface(mDbInterface).getInterfaceQuery(mDbQuery);
    try {
      //Evaluate binds but do NOT execute - this will be done by DownloadParcel
      List<ExecutableQuery> lQueryList = new ArrayList<>();
      DOMList lMatchList = lContextUElem.extendedXPathUL(mDbMatch, null);
      for (DOM lMatchNode : lMatchList) {
        ExecutableQuery lExecutableQuery = lQuery.createExecutableQuery(lMatchNode, lContextUElem);
        lQueryList.add(lExecutableQuery);
      }

      if (lQueryList.size() == 0) {
        Track.alert("ShowPopupNoMatch", "No query matches for query " + lQuery.getStatementName());
      }
      else {
        String lFileName;
        DownloadManager lDownloadManager = pRequestContext.getDownloadManager();
        DownloadParcel lNewDownloadParcel;

        if (!XFUtil.isNull(mZipArchiveName)) {
          lFileName = lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), mZipArchiveName);

          //Establish zip compression
          int lZipCompressionLevel = Deflater.DEFAULT_COMPRESSION;
          if (mZipCompressionLevel != null) { //No compression attribute set
            String lZipCompressionLevelPostXPath = lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), mZipCompressionLevel);
            if (!XFUtil.isNull(lZipCompressionLevelPostXPath)) {
              try {
                lZipCompressionLevel = Integer.valueOf(lZipCompressionLevelPostXPath);
              }
              catch (NumberFormatException e) {
                throw new ExInternal("Invalid number for command show-popup on attribute zip-compression-level.\n", e);
              }
            }
          }

          //Create a new download parcel for the zip file
          lNewDownloadParcel = lDownloadManager.addZipQueryDownload(lQueryList, lFileName, lZipCompressionLevel);
        }
        else {
          //Non-zip query; get the row and send it
          if (lQueryList.size() > 1) {
            throw new ExInternal("Non-zip download cannot match more than one node, matched " + lQueryList.size());
          }
          else {
            lFileName = lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), mFileName);
            lNewDownloadParcel = lDownloadManager.addQueryDownload(lQueryList.get(0), lFileName);
          }
        }

        //Use the DownloadDestination to establish the XDoResult type etc
        mDownloadDestination.addDownloadResult(pRequestContext, lNewDownloadParcel, lFileName);
      }
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to run XPath for fm:show-popup query command", e);
    }

    return XDoControlFlowContinue.instance();
  }

  @Override
  public boolean isCallTransition() {
    return false;
  }
}
