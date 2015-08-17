package net.foxopen.fox.thread.persistence.kryo;


import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import de.javakaffee.kryoserializers.EnumMapSerializer;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.track.Track;
import org.apache.commons.io.input.BoundedInputStream;
import org.objenesis.strategy.InstantiatorStrategy;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.ByteArrayOutputStream;
import java.util.EnumMap;


public class KryoManager {

  //Kryo doco: use the "default" strategy and only fall back to Objenesis if no no-arg constructor is available
  private static final InstantiatorStrategy INSTANTIATOR_STRATEGY = new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy());

  public static Kryo getKryoInstance() {

    Track.pushInfo("CreateKryoInstance");
    try {
      Kryo lKryo = new Kryo();

      //Note: Doco says all strategies are thread safe: http://objenesis.org/tutorial.html
      //Note: This introduces a dependency on the JVMs supported by Objenesis: http://code.google.com/p/objenesis/wiki/ListOfCurrentlySupportedVMs
      lKryo.setInstantiatorStrategy(INSTANTIATOR_STRATEGY);

      //Use the kryo-serializers library to handle unmodifiable collections (not supported by kryo out of the box).
      UnmodifiableCollectionsSerializer.registerSerializers(lKryo);
      lKryo.register(EnumMap.class, new EnumMapSerializer());

      //Register our custom DOM serialiser
      lKryo.register(DOM.class, new DOMSerializer());

      return lKryo;
    }
    finally {
      Track.pop("CreateKryoInstance");
    }
  }

  private KryoManager(){}

  /**
   * Overloaded serializer for DOM objects to improve performance and allow DocControl registration on deserialisation.
   */
  private static class DOMSerializer
  extends Serializer {

    @Override
    public void write(Kryo kryo, Output output, Object object) {
      //We need to serialise the DOM to a byte array so we know how big it is before it's written
      ByteArrayOutputStream lBAOS = new ByteArrayOutputStream();
      ((DOM) object).outputNodeToOutputStream(lBAOS, false, false);
      byte[] lBytes = lBAOS.toByteArray();
      //First write the array length so we know how long it will be when it's read back in
      output.writeInt(lBytes.length);
      output.write(lBytes);
    }

    @Override
    public Object read(Kryo kryo, Input input, Class type) {

      //First item to read is an integer representing the length of the written byte array
      int lDOMLength = input.readInt();

      //Restrict the InputStream to be read to the length of the serialised DOM, so the DOM constructor doesn't deplete the raw Kryo InputStream
      BoundedInputStream lBoundedInputStream = new BoundedInputStream(input, lDOMLength);

      return DOM.createDocument(lBoundedInputStream, false);
    }
  }
}
