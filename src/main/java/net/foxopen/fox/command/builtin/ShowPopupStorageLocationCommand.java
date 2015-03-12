package net.foxopen.fox.command.builtin;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.command.util.DownloadGeneratorDestination;
import net.foxopen.fox.command.util.GeneratorDestinationUtils;
import net.foxopen.fox.database.storage.lob.AnyLOBType;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.storage.FileStorageLocation;
import net.foxopen.fox.thread.storage.WorkingFileStorageLocation;

public class ShowPopupStorageLocationCommand
extends BuiltInCommand {

  /** The sotorage location of the ssi resource to retrieve. */
  private String mStorageLocation;

  private final DownloadGeneratorDestination mDownloadDestination;

  ShowPopupStorageLocationCommand(DOM pCommandDOM) {
    super(pCommandDOM);

    mStorageLocation = pCommandDOM.getAttrOrNull("storage-location");

    mDownloadDestination = GeneratorDestinationUtils.getDestinationFromShowPopupCommandMarkup(pCommandDOM, "", "");
  }

  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    //Get storage location
    FileStorageLocation lFSL = pRequestContext.getCurrentModule().getFileStorageLocation(mStorageLocation);
    //Evaluate SL binds
    WorkingFileStorageLocation lWFSL = lFSL.createWorkingStorageLocation(AnyLOBType.class, lContextUElem, true);

    mDownloadDestination.addDownloadResult(pRequestContext, lWFSL);

    return XDoControlFlowContinue.instance();
  }

  @Override
  public boolean isCallTransition() {
    return false;
  }
}
