package net.foxopen.fox.page;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExAttributeNotFound;
import net.foxopen.fox.ex.ExModule;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Resolves an attribute value from the DOM
 * @param <E> An enum of the attributes
 */
public class AttributeResolver<E extends Enum<E>> {
  private final Map<E, AttributeDefinition> mAttributeDefinitions;

  /**
   * Creates an attribute resolver for the specified attribute definitions
   * @param pAttributeDefinitions A map from an attribute to its definition
   */
  public AttributeResolver(Map<E, AttributeDefinition> pAttributeDefinitions) {
    mAttributeDefinitions = pAttributeDefinitions;
  }

  /**
   * Resolves the defined attributes to their values from the DOM. Any optional attributes that are not on the element
   * DOM are discarded from the result.
   * @param pElementDOM The DOM of the element with attributes to be resolved
   * @param pMapSupplier Supplies the attribute to value map (so an attribute enum map can be provided)
   * @return The resolved values from the DOM for the defined attributes
   * @throws ExModule If a required attribute could not be found on the element
   */
  public Map<E, String> resolveAttributes(DOM pElementDOM, Supplier<Map<E, String>> pMapSupplier)
  throws ExModule {
    // Get all the attributes on the element
    Map<String, String> lDOMAttributes = pElementDOM.getAttributeMap();

    try {
      // Convert the attribute definition from attribute enum -> definition to attribute enum -> resolved attribute
      // value, filtering any optional attributes not found away
      Map<E, String> lResolvedAttributes = pMapSupplier.get();
      mAttributeDefinitions.forEach((pAttribute, pDefinition) -> {
        resolveAttribute(lDOMAttributes, pDefinition).ifPresent(pResolvedAttribute -> {
          lResolvedAttributes.put(pAttribute, pResolvedAttribute);
        });
      });

      return lResolvedAttributes;
    }
    catch (ExAttributeNotFound e) {
      // An attribute marked as required was not found
      throw new ExModule("Required attribute '" + e.getAttributeDefinition().getName() + "' not found on element '" + pElementDOM.toString() + "'",
                         pElementDOM, e);
    }
  }

  /**
   * Returns the value of the defined attribute, resolved from the provided DOM attributes (a map from attribute name to
   * value). The returned value may be empty if the attribute definition is optional and not found in the DOM attributes.
   * @param pDOMAttributes The element DOM attributes, a map from attribute name to value
   * @param pAttributeDefinition The definition of the attribute to resolve
   * @return The value of the defined attribute, resolved from the provided DOM attributes
   * @throws ExAttributeNotFound If the attribute is not optional and could not be found
   */
  private Optional<String> resolveAttribute(Map<String, String> pDOMAttributes, AttributeDefinition pAttributeDefinition)
  throws ExAttributeNotFound {
    Optional<String> lAttribute = Optional.ofNullable(pDOMAttributes.get(pAttributeDefinition.getName()));

    if (pAttributeDefinition.isRequired() && XFUtil.isNull(lAttribute)) {
      throw new ExAttributeNotFound(pAttributeDefinition);
    }

    return lAttribute;
  }
}
