package net.foxopen.fox.thread.storage;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Implementors of this interface can generate a temporary resource just in time. They need to be independently serialisable
 * (i.e. not nested classes) and store all data which they need to generate output. They should NOT implicate complex object
 * graphs (i.e. all members should be primitives/Strings).<br><br>
 *
 * Currently a TempResourceGenerator is serialised to a binary {@link TempResource} using Kryo.
 */
public interface TempResourceGenerator {

  /**
   * Streams the contents of this TempResource to the given destination. Implementations should generate the output just in time
   * if possible.
   * @param pDestination Where output should be sent.
   * @throws IOException
   */
  void streamOutput(OutputStream pDestination) throws IOException;

  /**
   * @return The content type of this resource to be sent to the browser.
   */
  String getContentType();

  /**
   * @return The browser cache timeout in milliseconds.
   */
  long getCacheTimeMS();

}
