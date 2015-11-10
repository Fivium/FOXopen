package net.foxopen.fox.module.fieldset.fvm;

/**
 * Differentiates between the 2 types of "null" FieldSelectOption (key-missing and key-null), or marks a FieldSelectOption
 * as not null.
 */
public enum NullOptionType {
  NOT_NULL,
  KEY_MISSING,
  KEY_NULL
}
