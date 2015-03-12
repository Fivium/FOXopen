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
*
* @author Gary Watson
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
