/*

Copyright (c) 2013, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE - 
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
package net.foxopen.fox.xhtml;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test that this simple object behaves as expected.
 */
public class QueryStringSeparatorProviderTest {
  
  /**
   * Validate the basic desired outcome of the object.
   */
  @Test
  public void testBasicBehaviour() {
    QueryStringSeparatorProvider lSeparatorProvider = new QueryStringSeparatorProvider();
    Assert.assertEquals("First separator provided should be a question mark", "?", lSeparatorProvider.getSeparator());
    Assert.assertEquals("Second separator provided should be an ampersand", "&", lSeparatorProvider.getSeparator());
    Assert.assertEquals("Third separator provided should be an ampersand", "&", lSeparatorProvider.getSeparator());
  }
  
  /**
   * Test that newly constructed objects don't interfere with the first.
   */
  @Test
  public void testMultiInstanceBehaviour1() {
    QueryStringSeparatorProvider lSeparatorProvider = new QueryStringSeparatorProvider();
    Assert.assertEquals("First separator provided should be a question mark", "?", lSeparatorProvider.getSeparator());
    new QueryStringSeparatorProvider();
    Assert.assertEquals("Second separator provided should be an ampersand", "&", lSeparatorProvider.getSeparator());
    new QueryStringSeparatorProvider();
    Assert.assertEquals("Third separator provided should be an ampersand", "&", lSeparatorProvider.getSeparator());
  }
  
  /**
   * Test that newly constructed objects don't interfere with the first.
   */
  @Test
  public void testMultiInstanceBehaviour2() {
    QueryStringSeparatorProvider lSeparatorProvider1 = new QueryStringSeparatorProvider();
    QueryStringSeparatorProvider lSeparatorProvider2 = new QueryStringSeparatorProvider();
    Assert.assertEquals("First separator provided should be a question mark", "?", lSeparatorProvider1.getSeparator());
    Assert.assertEquals("First separator provided should be a question mark", "?", lSeparatorProvider2.getSeparator());
    Assert.assertEquals("Second separator provided should be an ampersand", "&", lSeparatorProvider1.getSeparator());
    Assert.assertEquals("Second separator provided should be an ampersand", "&", lSeparatorProvider2.getSeparator());
    Assert.assertEquals("Third separator provided should be an ampersand", "&", lSeparatorProvider1.getSeparator());
    Assert.assertEquals("Third separator provided should be an ampersand", "&", lSeparatorProvider2.getSeparator());
  }
  
}
