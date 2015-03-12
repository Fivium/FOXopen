/*

Copyright (c) 2012, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE -
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
package net.foxopen.fox.dom;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.dom.xpath.saxon.SaxonEnvironment;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.track.Track;

import net.sf.saxon.s9api.SAXDestination;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XQueryExecutable;
import net.sf.saxon.s9api.XdmNode;


/**
 * Utility class for running XQueries and populating a FOX DOM with the result.
 * Note this class does not currently cache compiled XQueries.
 */
public class XQueryUtil {

  /**
   * Run an XQuery statement against the input specified by pSourceDOM. Ouput is written to pDestinationDOM.
   * If pDestinationDOM has any existing children they are removed beforehand. :{contexts} are supported in the XQuery
   * String.
   * @param pSourceDOM The Source data for the XQuery.
   * @param pAttachPoint The initial context node (".") of the XQuery expression.
   * @param pDestinationDOM The Destination for the output.
   * @param pXQuery The XQuery String.
   * @param pContextUElem Used for resolving any :{contexts} which appear in the XQuery.
   */
  public static void runXQuery(DOM pSourceDOM, DOM pAttachPoint, DOM pDestinationDOM, String pXQuery, ContextUElem pContextUElem){

    XQueryCompiler lCompiler = SaxonEnvironment.getXQueryCompiler();

    //Rewrite :{context}s and the like
    pXQuery = SaxonEnvironment.replaceFoxMarkup(pXQuery, null);

    XQueryExecutable lExecutable;
    try {
      Track.pushDebug("XQueryCompile");
      lExecutable = lCompiler.compile(pXQuery);
    }
    catch (SaxonApiException e) {
      throw new ExInternal("Error compiling XQuery", e);
    }
    finally {
      Track.pop("XQueryCompile");
    }

    //PN TODO implement compiled XQuery cache
    XQueryEvaluator lEvaluator = lExecutable.load();

    pDestinationDOM.removeAllChildren();
    DOMContentHandler lContentHandler = new DOMContentHandler(pDestinationDOM);

    try {
      Track.pushDebug("XQueryPrepare");
      lEvaluator.setSource(pSourceDOM.wrap());
      lEvaluator.setContextItem(new XdmNode(pAttachPoint.wrap()));
      lEvaluator.setDestination(new SAXDestination(lContentHandler));
    }
    catch (SaxonApiException e) {
      throw new ExInternal("Error preparing XQuery", e);
    }
    finally {
      Track.pop("XQueryPrepare");
    }

    try {
      Track.pushDebug("XQueryExecute");
      SaxonEnvironment.setThreadLocalContextUElem(pContextUElem);
      lEvaluator.run();
    }
    catch (SaxonApiException e) {
      throw new ExInternal("Error running XQuery", e);
    }
    finally {
      Track.pop("XQueryExecute");
      SaxonEnvironment.clearThreadLocalContextUElem();
    }
  }

}
