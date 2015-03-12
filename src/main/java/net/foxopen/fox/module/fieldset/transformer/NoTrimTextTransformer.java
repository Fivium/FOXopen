package net.foxopen.fox.module.fieldset.transformer;


public class NoTrimTextTransformer
extends TextTransformer {

  NoTrimTextTransformer(CaseOption pCaseOption, String pInputMaskName) {
    super(pCaseOption, pInputMaskName);
  }

  @Override
  protected boolean trimPostedValue() {
    return false;
  }
}
