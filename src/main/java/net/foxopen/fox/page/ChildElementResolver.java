package net.foxopen.fox.page;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExChildElementNotFound;
import net.foxopen.fox.ex.ExModule;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Resolves a child element from the DOM
 * @param <E> An enum of the child elements
 */
public class ChildElementResolver<E extends Enum<E>> {
  private static final String CHILD_ELEMENTS_XPATH = "*";

  private final Map<E, ChildElementDefinition> mChildElementDefinitions;

  /**
   * Creates a child element resolver for the specified child element definitions
   * @param pChildElementDefinitions A map from a child element to its definition
   */
  public ChildElementResolver(Map<E, ChildElementDefinition> pChildElementDefinitions) {
    mChildElementDefinitions = pChildElementDefinitions;
  }

  /**
   * Resolves the defined child elements to their DOM values, searching underneath the specified parent element DOM. Any
   * optional child elements that are found underneath the parent element are discarded from the result.
   * @param pElementDOM The parent element DOM of the child elements to resolve
   * @param pMapSupplier Supplies the child element to value map (so a child element enum map can be provided)
   * @return The resolved DOM values for the defined child elements
   * @throws ExModule If a required child element could not be found underneath the parent element
   */
  public Map<E, DOM> resolveChildElements(DOM pElementDOM, Supplier<Map<E, DOM>> pMapSupplier)
  throws ExModule {
    // Check if there are any child elements underneath the element which are not in the child element definitions (i.e.
    // they are unexpected and therefore erroneous) - throw an exception with the name of the first unknown child element
    Optional<String> lFirstUndefinedChildElement = getFirstUndefinedChildElement(pElementDOM);
    if (lFirstUndefinedChildElement.isPresent()) {
      throw new ExModule("Undefined element '" + lFirstUndefinedChildElement.get() + "' found as a child of element '" + pElementDOM.toString() + "'",
                         pElementDOM);
    }

    try {
      // Convert the child element definitions from child element enum -> definition to child element enum -> resolved
      // element dom, filtering away any optional any child elements that weren't found
      Map<E, DOM> lResolvedChildElements = pMapSupplier.get();
      mChildElementDefinitions.forEach((pChildElement, pDefinition) -> {
        resolveChildElement(pElementDOM, pDefinition).ifPresent(pResolvedChildElement -> {
          lResolvedChildElements.put(pChildElement, pResolvedChildElement);
        });
      });

      return lResolvedChildElements;
    }
    catch (ExChildElementNotFound e) {
      // A child element marked as required was not found
      throw new ExModule("Required child element '" + e.getChildElementDefinition().getName() + "' not found underneath element '" + pElementDOM.toString() + "'",
                         pElementDOM, e);
    }
  }

  /**
   * Returns the name of the first child element underneath pElementDom that is not defined in the child element
   * definitions
   * @param pElementDOM The element DOM
   * @return The name of the first child element that is not defined in the child element definitions
   */
  private Optional<String> getFirstUndefinedChildElement(DOM pElementDOM) {
    return pElementDOM.getUL(CHILD_ELEMENTS_XPATH)
                      .stream()
                      .map(DOM::getName)
                      .filter(pChildElementName -> !getChildElementDefinitionNames().contains(pChildElementName))
                      .findFirst();
  }

  /**
   * Returns the names of the child elements defined for this resolver
   * @return The names of the child elements defined for this resolver
   */
  private List<String> getChildElementDefinitionNames() {
    return mChildElementDefinitions.values()
                                   .stream()
                                   .map(ChildElementDefinition::getName)
                                   .collect(Collectors.toList());
  }

  /**
   * Returns the DOM of the defined child element, resolved from the provided parent element DOM. The returned value may
   * be empty if the element definition is optional and could not be found under the parent element DOM.
   * @param pElementDOM The element DOM to search for the child under
   * @param pChildElementDefinition The definition of the child element to resolve
   * @return The DOM of the defined child element, resolved from the provided parent element DOM
   * @throws ExChildElementNotFound If the child element is not optional and could not be found
   */
  private Optional<DOM> resolveChildElement(DOM pElementDOM, ChildElementDefinition pChildElementDefinition)
  throws ExChildElementNotFound {
    Optional<DOM> lChildElement = Optional.ofNullable(pElementDOM.get1EOrNull(pChildElementDefinition.getName()));

    if (pChildElementDefinition.isRequired() && !lChildElement.isPresent()) {
      throw new ExChildElementNotFound(pChildElementDefinition);
    }

    return lChildElement;
  }
}
