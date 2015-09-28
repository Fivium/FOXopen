package net.foxopen.fox.thread.persistence.kryo;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import net.foxopen.fox.dom.DOM;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

public class KryoManagerTest {

  /**
   * Test to assert that DOM deserialisation does not attempt to close the underlying Kryo InputStream - this can cause
   * severe problems if it occurs. The InputStream close method is called by Xerces.
   */
  @Test
  public void testXMLDeserialiseDoesNotCloseInputStream() {

    class WrappingInputStream extends InputStream {

      private final InputStream mWrapped;

      public WrappingInputStream(InputStream pWrapped) {
        mWrapped = pWrapped;
      }

      @Override
      public int read()
      throws IOException {
        return mWrapped.read();
      }

      @Override
      public void close()
      throws IOException {
        throw new RuntimeException("Stream close attempt detected");
      }
    }

    //Create DOM and serialise to byte array
    DOM lDOMToSerialise = DOM.createDocumentFromXMLString("<ROOT/>");
    ByteArrayOutputStream lBAOS = new ByteArrayOutputStream();
    Output lOutput = new Output(lBAOS);
    KryoManager.getKryoInstance().writeObject(lOutput, lDOMToSerialise);
    lOutput.close();

    //Deserialise the DOM - if this calls close(), the WrappingInputStream will throw an exception, or Kryo will encounter a buffer underflow
    InputStream lInputStream = new WrappingInputStream(new ByteArrayInputStream(lBAOS.toByteArray()));
    DOM lDeserialisedDOM = KryoManager.getKryoInstance().readObject(new Input(lInputStream), DOM.class);

    assertEquals("DOM deserialised correctly", "<ROOT/>", lDeserialisedDOM.outputNodeToString(false));
  }

}