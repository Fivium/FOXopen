package net.foxopen.fox.module.datanode;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This class contains some public maps that designate how NodeAttributes are linked
 */
public class NodeAttributeAssociations {
  /**
   * Associated attributes have a key attribute and an attribute value which the key relies upon
   *
   * NOTE: There should be no chaining of association defined without re-writing the sorting code in NodeEvaluationContext
   */
  public static final Map<NodeAttribute, NodeAttribute> ASSOCIATED_ATTRIBUTES;
  static {
    Map<NodeAttribute, NodeAttribute> lAssociations = new HashMap<>();
    lAssociations.put(NodeAttribute.DESCRIPTION_BUFFER, NodeAttribute.DESCRIPTION_BUFFER_ATTACH_DOM);
    lAssociations.put(NodeAttribute.HINT_BUFFER, NodeAttribute.HINT_BUFFER_ATTACH_DOM);
    lAssociations.put(NodeAttribute.PHANTOM_BUFFER, NodeAttribute.PHANTOM_BUFFER_ATTACH_DOM);
    lAssociations.put(NodeAttribute.PROMPT_BUFFER, NodeAttribute.PROMPT_BUFFER_ATTACH_DOM);
    lAssociations.put(NodeAttribute.PROMPT_SHORT_BUFFER, NodeAttribute.PROMPT_SHORT_BUFFER_ATTACH_DOM);

    ASSOCIATED_ATTRIBUTES = Collections.unmodifiableMap(lAssociations);
  }

  /**
   * Map describing which sets of attributes should be mutually exclusive, if two attributes in a set are both defined for
   * a single element an error will be raised.
   */
  public static final Map<String, Collection<String>> MUTUALLY_EXCLUSIVE_ATTRIBUTES;
  static {
    Multimap<String, String> lMutuallyExclusiveAttributes = HashMultimap.create();
    lMutuallyExclusiveAttributes.put("Description content attributes", NodeAttribute.DESCRIPTION.getExternalString());
    lMutuallyExclusiveAttributes.put("Description content attributes", NodeAttribute.DESCRIPTION_BUFFER.getExternalString());

    lMutuallyExclusiveAttributes.put("Hint content attributes", NodeAttribute.HINT.getExternalString());
    lMutuallyExclusiveAttributes.put("Hint content attributes", NodeAttribute.HINT_BUFFER.getExternalString());

    lMutuallyExclusiveAttributes.put("Prompt content attributes", NodeAttribute.PROMPT.getExternalString());
    lMutuallyExclusiveAttributes.put("Prompt content attributes", NodeAttribute.PROMPT_BUFFER.getExternalString());

    lMutuallyExclusiveAttributes.put("Prompt Short content attributes", NodeAttribute.PROMPT_SHORT.getExternalString());
    lMutuallyExclusiveAttributes.put("Prompt Short content attributes", NodeAttribute.PROMPT_SHORT_BUFFER.getExternalString());

    MUTUALLY_EXCLUSIVE_ATTRIBUTES = Collections.unmodifiableMap(lMutuallyExclusiveAttributes.asMap());
  }
}
