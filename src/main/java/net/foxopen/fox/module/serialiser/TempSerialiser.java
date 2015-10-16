package net.foxopen.fox.module.serialiser;


/**
 * Interface to implement when making a temporary serialiser for general output serialisers.
 * Implementations should serialise to a temp store and then return the serialised content via getOutput() as well as
 * extending the OutputSerialiser to have the widget/component getters.
 * @param <T> The type of the serialiser output
 */
public interface TempSerialiser<T> extends OutputSerialiser {

  /**
   * Get the output that has been serialised up to this point to be output by another serialiser or cached
   *
   * @return Serialised output
   */
  T getOutput();
}
