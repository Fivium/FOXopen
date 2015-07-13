package net.foxopen.fox.database.storage.dom;

import net.foxopen.fox.ContextUCon;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDBTimeout;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.thread.storage.WorkingDataDOMStorageLocation;
import net.foxopen.fox.track.Track;

/**
 * An XMLWorkDoc which can only be read and not updated. This uses the same change number checking mechanism at the start
 * of every churn to ensure the latest version of the DOM is in memory, and reads it from the database if not. It is important
 * to note that a RO WorkDoc should ALWAYS contain a copy of the DOM which is seperate from its writeable counterpart - otherwise
 * updates to the writeable DOM could cause concurrency issues for read only consumers.<br/><br/>
 *
 * Unlike a WriteableXMLWorkDoc, this object maintains no state apart from the most recent XML document. It is always considered
 * "open" and cannot be locked. However, {@link #open} should still be invoked to ensure the cached document is up to date.
 */
public class ReadOnlyXMLWorkDoc
extends XMLWorkDoc {

  ReadOnlyXMLWorkDoc(WorkingDataDOMStorageLocation pWorkingStoreLocation, XMLWorkDocDOMAccessor pDOMAccessor) {
    super(pWorkingStoreLocation, pDOMAccessor);
  }

  @Override
  public void open(ContextUCon pContextUCon, boolean pRequiresValidation) {
    Track.pushInfo("ROWorkDocOpen", getDOMAccessor().getClass().getSimpleName());
    synchronized(this) {
      try {
        UCon lUCon = pContextUCon.getUCon("WorkDoc Open");
        try {
          XMLWorkDocSelectResult lSelectResult = selectRowAndOpenLocator(lUCon);
          if(!lSelectResult.rowExists()) {
            throw new ExInternal("Row not found and cannot be inserted for a read-only WorkDoc", lSelectResult.getSelectException());
          }
          //Populate the DOM member from the database row (only does a read if it has changed)
          readExistingRow(lUCon);

          //Set correct RO actuator on the DOM
          getDOM().getDocControl().setDocumentReadOnly();

          //Close the accessor immediately as we don't need to keep it open
          getDOMAccessor().closeLocator(lUCon);
        }
        catch (ExDBTimeout e) {
          throw new ExInternal("Timeout encountered while trying to open a read-only WorkDoc", e);
        }
        finally {
          pContextUCon.returnUCon(lUCon, "WorkDoc Open");
        }

        Track.info("WorkDocCacheKey", getCacheKey());
      }
      finally {
        Track.pop("ROWorkDocOpen");
      }
    }
  }

  @Override
  public void close(ContextUCon pContextUCon) {
  }

  @Override
  public void post(ContextUCon pContextUCon) {
    Track.info("SkipRODOMPost", "Skipping post of RO DOM");
  }

  @Override
  public void markAsValidated(boolean pIsValid) {
  }

  private void readExistingRow(UCon pUCon) {
    if(getDOMAccessor().isLocatorEmpty(pUCon) || getDOMAccessor().isLocatorNull(pUCon)) {
      //Experimental - it may be nicer to return a root-only XML document
      throw new ExInternal("XML LOB locator is null or empty and cannot be created for a read-only WorkDoc");
    }
    else {
      readNonEmptyExistingRow(pUCon);
    }
  }

  @Override
  public boolean isOpen() {
    return true;
  }

  @Override
  protected void abortInternal() {
  }

  @Override
  protected void acceptSelectColumnXML(DOM pSelectColumnXML) {
  }

  @Override
  public DOM getSelectColumnXMLOrNull() {
    return null;
  }
}
