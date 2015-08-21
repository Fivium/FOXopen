package net.foxopen.fox.configuration.resourcemaster.model;

import net.foxopen.fox.configuration.resourcemaster.definition.FoxEnvironmentDefinition;
import net.foxopen.fox.configuration.resourcemaster.definition.FoxEnvironmentProperty;
import net.foxopen.fox.ex.ExApp;

public class DatabaseProperties {

  private static final DatabaseProperties UNCONFIGURED_DEFAULT_INSTANCE = new DatabaseProperties(true, true);
  public static DatabaseProperties defaultForUnconfiguredEngine() {
    return UNCONFIGURED_DEFAULT_INSTANCE;
  }

  public static final String XML_STRATEGY_STANDARD = "standard";
  public static final String XML_STRATEGY_BINARY = "binary";

  private final boolean mUseBinaryXMLReader;
  private final boolean mUseBinaryXMLWriter;

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

    return new DatabaseProperties(XML_STRATEGY_BINARY.equals(lXMLReaderStrategy), XML_STRATEGY_BINARY.equals(lXMLWriterStrategy));
  }

  private DatabaseProperties(boolean pUseBinaryXMLReader, boolean pUseBinaryXMLWriter) {
    mUseBinaryXMLReader = pUseBinaryXMLReader;
    mUseBinaryXMLWriter = pUseBinaryXMLWriter;
  }

  public boolean isUseBinaryXMLReader() {
    return mUseBinaryXMLReader;
  }

  public boolean isUseBinaryXMLWriter() {
    return mUseBinaryXMLWriter;
  }
}
