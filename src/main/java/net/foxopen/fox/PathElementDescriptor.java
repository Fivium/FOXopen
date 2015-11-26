package net.foxopen.fox;
/**
* Describes an element of some arbitrary path.
*
* <p>Under this model, paths are comprised of a series of path elements,
* separated by a delimiter (.), that generally are of the form:
*
* <ul>
* <li>a
* <li>a.b.c
* <li>a.b[0].c
* <li>java.lang.Boolean
* </ul>
*/
public class PathElementDescriptor
{
  /** The name of the path element. */
  private String name;
  /** Determines whether the path element also includes an index part. */
  private boolean isIndexed;
  /** If the path element is indexed, the index specified. */
  private int index = -1;

  public PathElementDescriptor() {
     this("unknown");
  }

  public PathElementDescriptor(String name) {
    this.name      = name;
    this.isIndexed = false;
  }

  public PathElementDescriptor(String name, int index) {
    this.name      = name;
    this.index     = index;
    this.isIndexed = true;
  }

  public String getName() {
    return name;
  }

  public int getIndex() {
    return index;
  }

  public boolean getIsIndexed() {
    return isIndexed;
  }

  public String getCanonicalForm() {
    return getName()+(getIsIndexed() ? "["+getIndex()+"]" : "");
  }

  public String toString() {
    return getCanonicalForm();
  }
}
