package net.foxopen.fox.database.parser;

import net.foxopen.fox.database.parser.StatementParser.EscapeDelimiter;


/**
 * A segment of a ParsedStatement which contains unescaped text, i.e. not in a comment or quotation marks. This will
 * probably be treated as a SQL statement component by the Oracle parser. This is the "default" segment type of a statement;
 * statements are assumed to be unescaped until an escape sequence is encountered.
 */
class UnescapedTextSegment
extends StatementSegment {
  
  UnescapedTextSegment(int pStartIndex){
    super(pStartIndex);
  }

  StatementSegment consumeBuffer(String pBuffer) {
    
    //Index of the closest escape character
    int lClosestEscapeIdx = Integer.MAX_VALUE;    
    EscapeDelimiter lClosestEscapedDelimiter = null;
    char lWildcardQQuoteCharacter = '*';
        
    //Loop through the remaining buffer characters looking for the first escape delimiter
    CHAR_LOOP:    
    for(int i = 0; i < pBuffer.length(); i++){
      
      //Check the character (range) at the current index for an escape delimiter
      ESC_LOOP:
      for(EscapeDelimiter lEscapeDelimiter : EscapeDelimiter.values()){
        
        boolean lIsWildCard = lEscapeDelimiter == EscapeDelimiter.QQUOTE_WILDCARD;
        
        //Add an extra character to make the wildcard's length correct
        if(pBuffer.length() < i + lEscapeDelimiter.mStartSequence.length() + (lIsWildCard ? 1 : 0)){
          //If the remaining buffer isn't long enough to contain this escape delimiter, don't bother checking          
          continue ESC_LOOP;
        }
        
        if(pBuffer.substring(i, i + lEscapeDelimiter.mStartSequence.length()).equals(lEscapeDelimiter.mStartSequence)){
          //The character or character sequence at this index matches an escape delimiter - record the position and break out of the loop          
          if(lEscapeDelimiter == EscapeDelimiter.QQUOTE_WILDCARD) {
            //If this is the wildcard qquote, record the wildcard character
            lWildcardQQuoteCharacter = pBuffer.charAt(i + lEscapeDelimiter.mStartSequence.length());
          }
          lClosestEscapeIdx = i;
          lClosestEscapedDelimiter = lEscapeDelimiter;         
          break CHAR_LOOP;
        }
      } 
    }
        
    //Establish the index in the string which this undelimited section goes up to - whatever is closer out of the nearest
    //escape sequence or the end of the buffer (if there are no further delimiters)
    int lGoesUpTo = Math.min(lClosestEscapeIdx, pBuffer.length());
    
    //Set the contents of this unescaped segment
    setContents(pBuffer.substring(0, lGoesUpTo));
    
    //If there was an escape sequence matched
    if(lClosestEscapedDelimiter != null) {
    
      String lStartSequence = lClosestEscapedDelimiter.mStartSequence;
      String lEndSequence = lClosestEscapedDelimiter.mEndSequence;
      
      if(lClosestEscapedDelimiter == EscapeDelimiter.QQUOTE_WILDCARD) {
        lStartSequence += lWildcardQQuoteCharacter;
        lEndSequence = lWildcardQQuoteCharacter + lEndSequence;
      }
      //An escape sequence was found, return an escaped segment to continue the read from the index of the escape sequence
      return new EscapedTextSegment(lStartSequence, lEndSequence, lClosestEscapedDelimiter.mRequiresEndDelimiter, lGoesUpTo);   
    }
    else {
      //Whole string depleted, nothing else to read
      return null;
    }
  }
  

  /**
   * This segment is redundant if its contents has 0-length.
   * @return True if this is an empty segment.
   */
  boolean isRedundant() {
    return getContents().length() == 0;
  }
}
  
  