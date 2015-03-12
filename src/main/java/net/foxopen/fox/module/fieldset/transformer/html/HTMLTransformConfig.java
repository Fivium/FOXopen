package net.foxopen.fox.module.fieldset.transformer.html;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Configuration options for use by a {@link HTMLValueTransformer} when transforming HTML sent by the user into XML.
 *
 * Tag name and attribute matching is always case insensitive. Note all tag/attribute names provided to this class at construction time should be lower case.
 */
public class HTMLTransformConfig {

  /** Engine defaults for "standard" widgets */
  static final HTMLTransformConfig DEFAULT_STANDARD_INSTANCE = defaultConfig(true);
  /** Engine defaults for "expanded" widgets */
  static final HTMLTransformConfig DEFAULT_EXPANDED_INSTANCE = defaultConfig(false);

  private final Collection<String> mRemoveEmptyTagNames;
  private final Collection<String> mUnnestNestedTagNames;

  private final Collection<TagTranslation> mTagTranslations;
  private final Collection<String> mAllowedTagNames;

  private final Multimap<String, String> mTagNamesToAllowedAttributes;

  private final boolean mMailMergeTranslationEnabled;
  private final boolean mMSWordCleanseEnabled;

  private static HTMLTransformConfig defaultConfig(boolean pStandard) {

    Collection<String> lRemoveEmptyTagNames = Arrays.asList("b", "u", "i", "p");
    Collection<String> lUnnestNestedTagNames = Collections.singleton("p");

    Collection<TagTranslation> lTagTranslations = new ArrayList<>();
    lTagTranslations.add(new TagTranslation("b", "strong"));
    lTagTranslations.add(new TagTranslation("i", "em"));
    lTagTranslations.add(new TagTranslation("p", "h1", "h2", "h3", "h4", "h5", "h6", "div"));

    Collection<String> lAllowedTagNames;
    if(pStandard) {
      lAllowedTagNames = Sets.newHashSet("p", "br", "u", "mm", "ul", "li", "b", "i", "center", "font");
    }
    else {
      lAllowedTagNames = Sets.newHashSet("p", "br", "u", "mm", "ul", "li", "b", "i", "ol", "sup", "sub", "center", "font", "blockquote", "strike");
    }

    Multimap<String, String> lTagNamesToAllowedAttributes = HashMultimap.create();
    lTagNamesToAllowedAttributes.put("p", "align");

    //MM field translation should only apply to standard config (not expanded)
    return new HTMLTransformConfig(lRemoveEmptyTagNames, lUnnestNestedTagNames, lTagTranslations, lAllowedTagNames, lTagNamesToAllowedAttributes, pStandard, true);
  }

  public HTMLTransformConfig(Collection<String> pRemoveEmptyTagNames, Collection<String> pUnnestNestedTagNames,
                             Collection<TagTranslation> pTagTranslations, Collection<String> pAllowedTagNames,
                             Multimap<String, String> pTagNamesToAllowedAttributes,
                             boolean pMailMergeTranslationEnabled, boolean pMSWordCleanseEnabled) {
    mRemoveEmptyTagNames = pRemoveEmptyTagNames;
    mUnnestNestedTagNames = pUnnestNestedTagNames;
    mTagTranslations = pTagTranslations;
    mAllowedTagNames = pAllowedTagNames;
    mTagNamesToAllowedAttributes = pTagNamesToAllowedAttributes;
    mMailMergeTranslationEnabled = pMailMergeTranslationEnabled;
    mMSWordCleanseEnabled = pMSWordCleanseEnabled;
  }

  /**
   * Tests if the given tag name should be removed if it is empty.
   * @param pTagName Tag name to test.
   * @return
   */
  public boolean removeTagIfEmpty(String pTagName) {
    return mRemoveEmptyTagNames.contains(pTagName.toLowerCase());
  }

  /**
   * Tests if the given tagname should be unnested from its parent in the case that the parent is otherwise empty.
   * @param pTagName Tag name to test.
   * @return
   */
  public boolean unnestFromEmptyParent(String pTagName) {
    return mUnnestNestedTagNames.contains(pTagName.toLowerCase());
  }

  /**
   * Gets the TagTranslation rules for this config.
   * @return
   */
  public Collection<TagTranslation> getTagTranslations() {
    return mTagTranslations;
  }

  /**
   * Tests if the given tag name is allowed by this config.
   * @param pTagName Tag name to test.
   * @return
   */
  public boolean tagAllowed(String pTagName) {
    return mAllowedTagNames.contains(pTagName.toLowerCase());
  }

  /**
   * Tests if the given attribute name is allowed by this config.
   * @param pTagName Tag name to test.
   * @param pAttributeName Attribute name to test.
   * @return
   */
  public boolean attributeAllowed(String pTagName, String pAttributeName) {
    return mTagNamesToAllowedAttributes.get(pTagName.toLowerCase()).contains(pAttributeName.toLowerCase());
  }

  /**
   * Tests if [[MM_FIELD]] markup should be translated into a corresponding {@code <MM>MM_FIELD</MM>} element.
   * @return
   */
  public boolean isMailMergeTranslationEnabled() {
    return mMailMergeTranslationEnabled;
  }

  /**
   * Tests if the legacy MSWord cleansing regex should be applied to the HTML.
   * @return
   */
  public boolean isMSWordCleanseEnabled() {
    return mMSWordCleanseEnabled;
  }
}
