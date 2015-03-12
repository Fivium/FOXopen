package net.foxopen.fox.thread.persistence.kryo;


import com.esotericsoftware.kryo.Kryo;
import de.javakaffee.kryoserializers.EnumMapSerializer;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;
import org.objenesis.strategy.InstantiatorStrategy;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.util.EnumMap;


public class KryoManager {

  //Kryo doco: use the "default" strategy and only fall back to Objenesis if no no-arg constructor is available
  private static final InstantiatorStrategy INSTANTIATOR_STRATEGY = new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy());

  public static Kryo getKryoInstance() {

    Kryo lKryo = new Kryo();

    //Note: Doco says all strategies are thread safe: http://objenesis.org/tutorial.html
    //Note: This introduces a dependency on the JVMs supported by Objenesis: http://code.google.com/p/objenesis/wiki/ListOfCurrentlySupportedVMs
    lKryo.setInstantiatorStrategy(INSTANTIATOR_STRATEGY);

    //Use the kryo-serializers library to handle unmodifiable collections (not supported by kryo out of the box).
    UnmodifiableCollectionsSerializer.registerSerializers(lKryo);
    lKryo.register(EnumMap.class, new EnumMapSerializer());

    return lKryo;
  }

  private KryoManager(){}
}
