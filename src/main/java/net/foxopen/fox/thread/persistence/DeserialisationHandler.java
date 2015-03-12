package net.foxopen.fox.thread.persistence;

/**
 * Implementors of this interface indicate that they require a notification to be sent after they are deserialised by
 * a {@link Deserialiser}. They may use this to perform additional initialisation, for instance re-creating transient
 * fields which may not have been serialised.
 */
public interface DeserialisationHandler {

  /**
   * Invoked immediately following the object's deserialisation. Any additional initiatialisation should be performed
   * in this method.
   */
  public void handleDeserialisation();

}
