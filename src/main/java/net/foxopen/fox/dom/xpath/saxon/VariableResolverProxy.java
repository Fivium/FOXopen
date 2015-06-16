package net.foxopen.fox.dom.xpath.saxon;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.track.Track;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathVariableResolver;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Singleton implementation of an XPathVariableResolver which uses the Saxon ThreadLocal RequestContext to retrieve an
 * {@link XPathVariableManager} to delegate to.
 */
public class VariableResolverProxy
implements XPathVariableResolver {

  static VariableResolverProxy INSTANCE = new VariableResolverProxy();

  private VariableResolverProxy() {
  }

  @Override
  public Object resolveVariable(QName pVariableName) {

    XPathVariableManager lResolver = SaxonEnvironment.getThreadLocalRequestContext().getXPathVariableManager();
    String lVariableName = pVariableName.getLocalPart();

    //Get the raw variable object from the resolver
    Object lVariable = lResolver.resolveVariable(lVariableName);

    //DOMs and DOMs within lists need wrapping - lists should not be nested at this point so no need for recursion
    if(lVariable == null) {
      //Log undefined variable access for debug purposes
      Track.debug("UndefinedVariable", lVariableName);
      //Saxon casts null values to an empty sequence
      return null;
    }
    else if(lVariable instanceof DOM) {
      return ((DOM) lVariable).wrap();
    }
    else if(lVariable instanceof Collection) {
      return ((Collection<?>) lVariable)
        .stream()
        .map(e -> (e instanceof DOM) ? ((DOM) e).wrap() : e)
        .collect(Collectors.toList());
    }
    else {
      return lVariable;
    }
  }
}
