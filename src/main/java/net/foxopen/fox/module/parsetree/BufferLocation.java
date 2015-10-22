package net.foxopen.fox.module.parsetree;

/**
 * A buffer name defined in an attribute throughout a module might be in the form of just a buffer name or a forward
 * slash separated state & buffer name tuple which can be represented by this interface
 */
public interface BufferLocation {
  String getStateName();

  String getBufferName();
}
