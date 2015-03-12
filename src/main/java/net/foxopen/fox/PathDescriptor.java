/*

Copyright (c) 2010, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE -
                    ENERGY DEVELOPMENT UNIT (IT UNIT)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the DEPARTMENT OF ENERGY AND CLIMATE CHANGE nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

$Id$

*/
package net.foxopen.fox;

import java.util.StringTokenizer;

/**
 * Describes an arbitrary path in terms of its constituate elements.
 *
 * <p>Under this model, paths are comprised of a series of path elements. For
 * example:
 *
 * <ul>
 * <li>a
 * <li>a.b.c
 * <li>a.b[0].c
 * <li>java.lang.Boolean
 * </ul>
 *
 * @author Gary Watson
 */
public class PathDescriptor
{
  /** An array of the path element descriptors that comrise this path. */
  private PathElementDescriptor elementDescriptors[];

  /** The path. */
  private String path;

  /** A list of path element delimiter characters. */
  private String pathElementDelimiters = ".";

  public PathDescriptor(String path) {
    setPath(path);
  }

  public PathDescriptor(String path, String pathElementDelimiters) {
    setPathElementDelimiters(pathElementDelimiters);
    setPath(path);
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
    elementDescriptors = describePath(path);
  }

  public int getNumberOfPathElements() {
    return elementDescriptors.length;
  }

  public String getPathElementDelimiters() {
    return pathElementDelimiters;
  }

  public void setPathElementDelimiters(String delimiters) {
    pathElementDelimiters = delimiters;
  }

  private PathElementDescriptor[] describePath(String path) {
    StringTokenizer elementTok = new StringTokenizer(path, getPathElementDelimiters());
    PathElementDescriptor elements[] = new PathElementDescriptor[elementTok.countTokens()];

    for (int e=0; e < elements.length; e++) {
      String elementName  = elementTok.nextToken();
      int    elementIndex = -1;

      if (elementName.endsWith("]")) {
        int closeBracketPos = elementName.length()-1;
        int openBracketPos  = elementName.lastIndexOf("[");
        if (openBracketPos >= 0 && (closeBracketPos-openBracketPos > 1)) {
          try {
            elementIndex = Integer.valueOf(elementName.substring(openBracketPos+1, closeBracketPos)).intValue();
            elementName  = elementName.substring(0, openBracketPos);
            elements[e]  = new PathElementDescriptor(elementName, elementIndex);
          }
          catch (NumberFormatException ex) {
            // Safe to ignore this - it will be picked up by caller
          }
        }
      }
      else {
        elements[e]  = new PathElementDescriptor(elementName);
      }
    }

    return elements;
  }

  public PathElementDescriptor getPathElement(int index) {
    return elementDescriptors[index];
  }

  public PathElementDescriptor[] getPathElements() {
    return elementDescriptors;
  }

  public PathDescriptor getParentPathDescriptor() {
    int lastDelimiterPos = -1;

    for (int c=path.length()-1; c >=0 && lastDelimiterPos < 0; c--) {
      if ( pathElementDelimiters.indexOf(path.charAt(c)) >= 0) {
        lastDelimiterPos = c;
      }
    }

    if ( lastDelimiterPos >= 0) {
      return new PathDescriptor(path.substring(0, lastDelimiterPos), pathElementDelimiters);
    }
    else {
      return new PathDescriptor("", pathElementDelimiters);
    }
  }

  public PathElementDescriptor getLastPathElement() {
    return (elementDescriptors.length > 0 ? elementDescriptors[elementDescriptors.length-1] : null);
  }

  public PathDescriptor getTailPathDescriptor() {
    if ( elementDescriptors.length >= 2) {
      String headElement  = elementDescriptors[0].getCanonicalForm();
      int tailStartPos = headElement.length()+1; // i.e. a/b/c -> b/c
      tailStartPos = (pathElementDelimiters.indexOf(path.charAt(0)) >=0 ? tailStartPos+1 : tailStartPos); // i.e. /a/b/c -> b/c
      return new PathDescriptor(path.substring(tailStartPos), pathElementDelimiters);
    }
    else {
      return new PathDescriptor("", pathElementDelimiters);
    }
  }

  public PathDescriptor toRelativePath(PathDescriptor parentPath) {
    PathDescriptor relativePath = this;
    for (int n=0;
        parentPath.getNumberOfPathElements() > n &&
        relativePath.getNumberOfPathElements() > 0 &&
        parentPath.getPathElement(n).getCanonicalForm().equals(relativePath.getPathElement(0).getCanonicalForm());
        n++) {
      relativePath = relativePath.getTailPathDescriptor();
    }

    return relativePath;
  }

  public PathDescriptor toRelativePath(String parentPath) {
    return toRelativePath(new PathDescriptor(parentPath));
  }

  public String toString() {
    String className = getClass().getName();
    className = (className.lastIndexOf('.') >= 0 ? className.substring(className.lastIndexOf('.')+1) : className);
    StringBuffer sBuf = new StringBuffer();
    sBuf.append(className+"@"+hashCode()+"=");

    for (int n=0; n < elementDescriptors.length; n++) {
      sBuf.append(elementDescriptors[n]).append(n < elementDescriptors.length-1 ? "->" : "");
    }

    return sBuf.toString();
  }
}
