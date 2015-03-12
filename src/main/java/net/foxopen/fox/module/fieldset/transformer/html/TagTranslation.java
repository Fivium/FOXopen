package net.foxopen.fox.module.fieldset.transformer.html;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A "TagTranslation" rule for use in an HTMLValueTransformer. Tag names in the source collection should be translated into
 * the target tag name.
 */
class TagTranslation {

  private final String mTargetTagName;
  private final Set<String> mSourceTagNames;

  TagTranslation(String pTargetTagName, String... pSourceTagNames) {
    mTargetTagName = pTargetTagName;
    mSourceTagNames = new HashSet<>(Arrays.asList(pSourceTagNames));
  }

  /**
   * Tests if the given tag name requires translation to this rules' target name.
   * @param pTagName Source tag name.
   * @return True if tag requires translating.
   */
  public boolean requiresTranslation(String pTagName) {
    return mSourceTagNames.contains(pTagName.toLowerCase());
  }

  /**
   * Gets the target tag name for this translation rule.
   * @return
   */
  public String getTargetTagName() {
    return mTargetTagName;
  }
}
