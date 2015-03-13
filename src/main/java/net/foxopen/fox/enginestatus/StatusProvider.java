package net.foxopen.fox.enginestatus;

/**
 * Objects which can provide status messages for a status category.
 */
public interface StatusProvider {

  /**
   * Refreshes all the transient messages in this status category. Implementations should add messages etc to the given
   * destination.
   * @param pDestination Destination for refreshed messages.
   */
  void refreshStatus(StatusDestination pDestination);

  String getCategoryTitle();

  String getCategoryMnemonic();

  boolean isCategoryExpandedByDefault();

}
