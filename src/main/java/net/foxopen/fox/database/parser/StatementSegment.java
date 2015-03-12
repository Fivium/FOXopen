package net.foxopen.fox.database.parser;


import net.foxopen.fox.ex.ExParser;

/**
 * A section of a {@link ParsedStatement}, encapsulating the section's contents. These segments are primarily used to
 * differentiate between escaped and unescaped content in a SQL script, and perform further operations on certain parts of
 * the script. For instance you would not want to replace bind variable strings within comments, so ScriptSegments allow
 * only the relevant parts of the script to be replaced.<br/><br/>
 *
 * ScriptSegments are created by parsing a single script using the {@link StatementParser}class. They are mutable after
 * creation, as their contents can be rewritten or replaced as described above.
 */
abstract class StatementSegment{
  
  private String mContents = "";
  
  private final int mStartIndex;
  
  protected StatementSegment(int pStartIndex){
    mStartIndex = pStartIndex;
  }
  
  /**
   * Reads as much of the given string as possible before a character sequence which terminates the segment is encountered.
   * The object returned will be the ScriptSegment which should be used to continue the read from the position specified
   * by {@link #getStartIndex()}, or null if the end of the buffer has been reached.
   * @param pBuffer Buffer to read.
   * @return ScriptSegment for reading the next part of the buffer, or null.
   * @throws ExParser If an unterminated sequence is encountered.
   */
  abstract StatementSegment consumeBuffer(String pBuffer)
  throws ExParser;
  
  /**
   * Serialises this segment into the given Builder. The output of this object's serialisation should reflect exactly
   * the input which was used to create it.
   * @param pBuilder Build to serialise to.
   */
  void serialiseTo(StringBuilder pBuilder){
    pBuilder.append(mContents);
  }
  
  /**
   * Gets the contents of this segment, excluding any surronding escape sequences. I.e. for the input <tt>"q'{text}'</tt>
   * this method would return <tt>text</tt>.
   * @return Segment contents.
   */
  String getContents(){
    return mContents;
  }
  
  /**
   * Sets the contents of this segment.
   * @param pContents Contents.
   */
  protected void setContents(String pContents){
    mContents = pContents;
  }
  
  /**
   * Tests if this ScriptSegment is 'redundant', i.e. it would have no effect whatsoever if it was part of a ParsedStatement.
   * Currently this is only true for UnescapedTextSegments which have an untrimmed content length of 0.
   * @return True if this segment is redundant.
   */
  boolean isRedundant(){
    return false;
  }

  /**
   * Gets the index in the original buffer where this segment starts. The read operation for this segment should start
   * from this index.
   * @return Start index of this segment.
   */
  int getStartIndex() {
    return mStartIndex;
  }
}
