package net.foxopen.fox.track;


/**
 * Different serialisation types for TrackEntries.
 */
public enum TrackEntryType {
  /** A short string value to be serialised into the DATA attribute */
  DEFAULT,
  /** A short string value to be serialised in a custom attribute */
  ATTRIBUTE,
  /** A large block of text to be serialised as the text content of the element */
  TEXT,
  /** As with TEXT but not escaped (for XML strings) */
  XML;
}
