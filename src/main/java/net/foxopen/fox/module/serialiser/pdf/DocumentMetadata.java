package net.foxopen.fox.module.serialiser.pdf;

public class DocumentMetadata {
  private final String mTitle;
  private final String mAuthor;
  private final String mSubject;
  private final String mKeywords;

  public DocumentMetadata(String pTitle, String pAuthor, String pSubject, String pKeywords) {
    mTitle = pTitle;
    mAuthor = pAuthor;
    mSubject = pSubject;
    mKeywords = pKeywords;
  }

  public String getTitle() {
    return mTitle;
  }

  public String getAuthor() {
    return mAuthor;
  }

  public String getSubject() {
    return mSubject;
  }

  public String getKeywords() {
    return mKeywords;
  }
}
