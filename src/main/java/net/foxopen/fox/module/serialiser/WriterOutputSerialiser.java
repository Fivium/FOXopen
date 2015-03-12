package net.foxopen.fox.module.serialiser;

import net.foxopen.fox.ex.ExInternal;

import java.io.IOException;
import java.io.Writer;


/**
 * Generic output serialiser implementation with all possible widgets and components mapped to an unimplemented builder
 */
public abstract class WriterOutputSerialiser implements OutputSerialiser {

  protected Writer mWriter;

  protected WriterOutputSerialiser() {
  }

  protected void setWriter(Writer pWriter) {
    mWriter = pWriter;
  }

  public Writer getWriter() {
    return mWriter;
  }

  public WriterOutputSerialiser append(String pString) {
    if (mWriter == null) {
      throw new ExInternal("Cannot append to WriterOutputSerialiser without calling setWriter() first");
    }

    if (pString != null) {
      try {
        mWriter.write(pString);
      }
      catch (IOException e) {
        throw new ExInternal("Appending \"" + pString + "\" to response writer failed", e);
      }
    }
    return this;
  }
}
