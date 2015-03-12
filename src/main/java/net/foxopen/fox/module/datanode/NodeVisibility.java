package net.foxopen.fox.module.datanode;

/**
 * Visibility enumeration for display nodes. Set via ro/edit attributes
 */
public enum NodeVisibility {
  DENIED(0)
, VIEW(1)
, EDIT(2);

  private final int mLevel;

  private NodeVisibility(int pLevel) {
    mLevel = pLevel;
  }

  /**
   * Returns this NodeVisibility expressed as a number. Higher numbers indicate a More visible/editable node.
   */
  public int asInt(){
    return mLevel;
  }
}
