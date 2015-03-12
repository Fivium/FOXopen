package net.foxopen.fox.configuration.resourcemaster.model;

import net.foxopen.fox.configuration.resourcemaster.definition.FoxEnvironmentDefinition;
import net.foxopen.fox.configuration.resourcemaster.definition.FoxEnvironmentProperty;
import net.foxopen.fox.ex.ExApp;

public class DatabaseProperties {

  public static final String XML_READER_STRATEGY_STANDARD = "standard";
  public static final String XML_READER_STRATEGY_BINARY = "binary";

  private final boolean mUseBinaryXMLReader;

  public static DatabaseProperties createDatabaseProperties(FoxEnvironmentDefinition pFoxEnvironmentDefinition)
  throws ExApp {
    //Default strategy should be to use the binary reader unless it's not supported by the database
    String lXMLReaderStrategy = pFoxEnvironmentDefinition.getPropertyAsString(FoxEnvironmentProperty.DATABASE_BINARY_XML_READER_STRATEGY);

    if(!XML_READER_STRATEGY_STANDARD.equals(lXMLReaderStrategy) && !XML_READER_STRATEGY_BINARY.equals(lXMLReaderStrategy)) {
      throw new ExApp("binary-xml-reader-strategy must be 'standard' or 'binary'");
    }

    return new DatabaseProperties(XML_READER_STRATEGY_BINARY.equals(lXMLReaderStrategy));
  }

  private DatabaseProperties(boolean pUseBinaryXMLReader) {
    mUseBinaryXMLReader = pUseBinaryXMLReader;
  }

  public boolean isUseBinaryXMLReader() {
    return mUseBinaryXMLReader;
  }
}
