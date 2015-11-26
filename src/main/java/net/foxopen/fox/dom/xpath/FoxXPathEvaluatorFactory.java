package net.foxopen.fox.dom.xpath;


/**
 * Static factory object for the creation of a {@link FoxXPathEvaluator}.
 * This should be used sparingly as the created FoxXPathEvaluator should be cacheable and reusable.
 */
public class FoxXPathEvaluatorFactory {

  static enum EvaluatorType{
    STANDARD
  }

  /**
   * Instantiation not allowed.
   */
  private FoxXPathEvaluatorFactory(){}

  private static final EvaluatorType DEFAULT_EVALUATOR_TYPE = EvaluatorType.STANDARD;

  /**
   * Create a new FoxXPathEvaluator using the default EvaluatorType (DEFAULT).
   * @param pUseXPathBackwardsCompatibility If true, the FoxXPathEvaluator will operate in XPath 1.0 compatibility mode.
   * @return A new XPath evaluator.
   */
  public static FoxXPathEvaluator createEvaluator(boolean pUseXPathBackwardsCompatibility){
    return createEvaluator(DEFAULT_EVALUATOR_TYPE, pUseXPathBackwardsCompatibility);
  }

  /**
   * Create a new FoxXPathEvaluator of the given EvaluatorType.
   * @param pUseXPathBackwardsCompatibility If true, the FoxXPathEvaluator will operate in XPath 1.0 compatibility mode.
   * @param pEvaluatorType The type of FoxXPathEvaluator to create.
   * @return A new XPath evaluator.
   */
  public static FoxXPathEvaluator createEvaluator(EvaluatorType pEvaluatorType, boolean pUseXPathBackwardsCompatibility){
    switch(pEvaluatorType){
      case STANDARD:
        return new FoxXPathEvaluator(pUseXPathBackwardsCompatibility);
    }
    return null;
  }

}
