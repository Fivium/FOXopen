package net.foxopen.fox.database.parser;


import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExParser;


/**
 * Segment of a SQL statement which is "escaped" in some way, i.e. part of a comment block or within quotation marks.
 */
class EscapedTextSegment
extends StatementSegment {
  
  final String mStartSeqeunce;
  final String mEndSequence;
  final boolean mRequiresEndDelimiter;
  
  public EscapedTextSegment(String pStartSeqeunce, String pEndSeqeunce, boolean pRequiresEndDelimiter, int pStartIndex) {
    super(pStartIndex);
    mStartSeqeunce = pStartSeqeunce;
    mEndSequence = pEndSeqeunce;
    mRequiresEndDelimiter = pRequiresEndDelimiter;
  }

  StatementSegment consumeBuffer(String pBuffer) 
  throws ExParser {
    
    //Sanity check that the buffer starts with the correct sequence
    if(pBuffer.indexOf(mStartSeqeunce) != 0){
      //This is an internal error, this method should not have been called if the buffer is not in the correct state
      throw new ExInternal("Sequence " + mStartSeqeunce + " should start at position 0");
    }
    
    //Find the corresponding terminating sequence
    int lEndPosition = pBuffer.indexOf(mEndSequence, 1);
    int lEndEscSeqLength = mEndSequence.length();
    
    //If the remaining string doesn't contain this segment's termination sequence, that is a problem
    if(lEndPosition == -1){
      if(mRequiresEndDelimiter){
        throw new ExParser("Unterminated escape sequence " + mStartSeqeunce); 
      }
      else {
        //For "--" sequence, end of file counts as a terminator
        lEndPosition = pBuffer.length()-1;
      }
    }
    
    //Record the contents of this segment
    setContents(pBuffer.substring(mStartSeqeunce.length(), lEndPosition));
    
    //Return a new unescaped segment which will start reading from the end of this segment
    return new UnescapedTextSegment(lEndPosition + lEndEscSeqLength); 
  }

  void serialiseTo(StringBuilder pBuilder) {
    pBuilder.append(mStartSeqeunce);
    pBuilder.append(getContents());
    pBuilder.append(mEndSequence);
  }

  
  /**
   * Tests if this segment is a single or multi line comment.
   * @return True if this segment represents a SQL comment.
   */
  public boolean isComment(){
    return mStartSeqeunce.equals(StatementParser.EscapeDelimiter.COMMENT_MULTILINE.mStartSequence) || mStartSeqeunce.equals(StatementParser.EscapeDelimiter.COMMENT_SINGLELINE.mStartSequence);
  }

}
