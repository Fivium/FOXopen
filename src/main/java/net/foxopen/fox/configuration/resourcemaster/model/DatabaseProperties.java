package net.foxopen.fox.configuration.resourcemaster.model;

import net.foxopen.fox.configuration.resourcemaster.definition.FoxEnvironmentDefinition;
import net.foxopen.fox.configuration.resourcemaster.definition.FoxEnvironmentProperty;
import net.foxopen.fox.ex.ExApp;

public class DatabaseProperties {

  private static final DatabaseProperties UNCONFIGURED_DEFAULT_INSTANCE = new DatabaseProperties(true, true, true);
  public static DatabaseProperties defaultForUnconfiguredEngine() {
    return UNCONFIGURED_DEFAULT_INSTANCE;
  }

  public static final String XML_STRATEGY_STANDARD = "standard";
  public static final String XML_STRATEGY_BINARY = "binary";

  public static final String STANDARD_XML_WRITER_METHOD_BYTES = "bytes";
  public static final String STANDARD_XML_WRITER_METHOD_CHARACTERS = "characters";

  private final boolean mUseBinaryXMLReader;
  private final boolean mUseBinaryXMLWriter;
  private final boolean mSendBytesToStandardXMLWriter;

  public static DatabaseProperties createDatabaseProperties(FoxEnvironmentDefinition pFoxEnvironmentDefinition)
  throws ExApp {
    //Default strategy should be to use the binary reader unless it's not supported by the database
    String lXMLReaderStrategy = pFoxEnvironmentDefinition.getPropertyAsString(FoxEnvironmentProperty.DATABASE_BINARY_XML_READER_STRATEGY);
    if(!XML_STRATEGY_STANDARD.equals(lXMLReaderStrategy) && !XML_STRATEGY_BINARY.equals(lXMLReaderStrategy)) {
      throw new ExApp("binary-xml-reader-strategy must be 'standard' or 'binary'");
    }

    String lXMLWriterStrategy = pFoxEnvironmentDefinition.getPropertyAsString(FoxEnvironmentProperty.DATABASE_BINARY_XML_WRITER_STRATEGY);
    if(!XML_STRATEGY_STANDARD.equals(lXMLWriterStrategy) && !XML_STRATEGY_BINARY.equals(lXMLWriterStrategy)) {
      throw new ExApp("binary-xml-writer-strategy must be 'standard' or 'binary'");
    }

    String lStandardXMLWriterMethod = pFoxEnvironmentDefinition.getPropertyAsString(FoxEnvironmentProperty.DATABASE_STANDARD_XML_WRITER_METHOD);
    if(!STANDARD_XML_WRITER_METHOD_BYTES.equals(lStandardXMLWriterMethod) && !STANDARD_XML_WRITER_METHOD_CHARACTERS.equals(lStandardXMLWriterMethod)) {
      throw new ExApp("standard-xml-writer-method must be 'characters' or 'bytes'");
    }

    return new DatabaseProperties(XML_STRATEGY_BINARY.equals(lXMLReaderStrategy), XML_STRATEGY_BINARY.equals(lXMLWriterStrategy), STANDARD_XML_WRITER_METHOD_BYTES.equals(lStandardXMLWriterMethod));
  }

  private DatabaseProperties(boolean pUseBinaryXMLReader, boolean pUseBinaryXMLWriter, boolean pSendBytesToStandardXMLWriter) {
    mUseBinaryXMLReader = pUseBinaryXMLReader;
    mUseBinaryXMLWriter = pUseBinaryXMLWriter;
    mSendBytesToStandardXMLWriter = pSendBytesToStandardXMLWriter;
  }

  public boolean isUseBinaryXMLReader() {
    return mUseBinaryXMLReader;
  }

  public boolean isUseBinaryXMLWriter() {
    return mUseBinaryXMLWriter;
  }

  public boolean isSendBytesToStandardXMLWriter() {
    return mSendBytesToStandardXMLWriter;
  }
}
