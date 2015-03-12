package net.foxopen.fox.module.serialiser;


/**
 * Interface to implement when making a temporary serialiser for general output serialisers.
 * Implementations should serialise to a temp store and then return the serialised content via getOutput() as well as
 * extending the OutputSerialiser to have the widget/component getters.
 */
public interface TempSerialiser extends OutputSerialiser {

  /**
   * Get the output that has been serialised up to this point as a string to be output by another serialiser or cached
   *
   * @return Serialised output String
   */
  public String getOutput();
}
