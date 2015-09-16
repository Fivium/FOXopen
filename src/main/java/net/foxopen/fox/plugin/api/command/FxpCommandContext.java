package net.foxopen.fox.plugin.api.command;

import net.foxopen.fox.plugin.api.PluginManagerContext;
import net.foxopen.fox.plugin.api.command.util.FxpConsumer;
import net.foxopen.fox.plugin.api.command.util.FxpGenerator;
import net.foxopen.fox.plugin.api.database.FxpContextUCon;
import net.foxopen.fox.plugin.api.dom.FxpContextUElem;
import net.foxopen.fox.plugin.api.dom.FxpDOM;
import net.foxopen.fox.plugin.api.dom.FxpDOMList;

public interface FxpCommandContext {

  /**
   * Gets a ContextUElem containing all the DOMs and labels currently available in the churn.
   * @return
   */
  FxpContextUElem<FxpDOM, FxpDOMList> getContextUElem();

  /**
   * Gets a ContextUCon for the excecution of arbitrary queries.
   * @return
   */
  FxpContextUCon getContextUCon();

  /**
   * Get the plugin manager context containg configuration information for the plugin
   * @return
   */
   PluginManagerContext getPluginManagerContext();

  /**
   * Performs a read of the given storage location. The sub-interface of the Consumer must correspond to the LOB type of the
   * storage location, i.e. FxpInputStreamConsumer for Blobs or FxpReaderConsumer for Clobs.
   * @param pStorageLocationName Valid storage location name. An error is thrown if the SL cannot be found.
   * @param pConsumer Object which will perform the SL read.
   */
  void readStorageLocation(String pStorageLocationName, FxpConsumer pConsumer);

  /**
   * Reads data from the given XML storage location into a writeable, non-persisted DOM. Standard WSL selection logic
   * is used to retrieve the DOM - an erorr is raised if no row could be found or if the XML column is null.
   * @param pStorageLocationName Valid storage location name. An error is thrown if the SL cannot be found.
   * @return Writeable copy of the SL DOM.
   */
  FxpDOM readXMLStorageLocation(String pStorageLocationName);

  /**
   * Invokes a file download from the plugin.
   * @param pFileName Filename of file to be downloaded.
   * @param pContentType Mime type of the download.
   * @param pServeAsAttachmentXPath An XPath which is executed to determine if the download should be served with a content
   * disposition of "attachment". Default is false if null.
   * @param pGenerator An OutputStreamGenerator or WriterGenerator which will generate the download's output.
   */
  void writeToDownload(String pFileName, String pContentType, String pServeAsAttachmentXPath, FxpGenerator pGenerator);

  /**
   * Writes data to a storage location. The sub-interface of the Generator must correspond to the LOB type of the
   * storage location, i.e. FxpOutputStreamGenerator for Blobs or FxpWriterGenerator for Clobs.
   * @param pStorageLocationName Valid storage location name. An error is thrown if the SL cannot be found.
   * @param pGenerator A generator which will write output to the storage location.
   */
  void writeToStorageLocation(String pStorageLocationName, FxpGenerator pGenerator);

}
