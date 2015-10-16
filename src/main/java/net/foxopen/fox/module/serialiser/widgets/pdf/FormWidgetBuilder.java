package net.foxopen.fox.module.serialiser.widgets.pdf;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;

/**
 * Serialises a form widget
 */
public class FormWidgetBuilder {
  /**
   * The node class attributes that should be applied when serialising a part of the form
   */
  private static final Multimap<FormWidget.FormPart, NodeAttribute> FORM_PART_CLASS_ATTRIBUTES = LinkedListMultimap.create();
  /**
   * The node style attributes that should be applied serialising a part of the form
   */
  private static final Multimap<FormWidget.FormPart, NodeAttribute> FORM_PART_STYLE_ATTRIBUTES = LinkedListMultimap.create();
  private static final WidgetBuilder<PDFSerialiser, EvaluatedNodeInfo> INSTANCE = new FormWidget(FORM_PART_CLASS_ATTRIBUTES, FORM_PART_STYLE_ATTRIBUTES);

  static {
    FORM_PART_CLASS_ATTRIBUTES.put(FormWidget.FormPart.FORM, NodeAttribute.CLASS);
    FORM_PART_CLASS_ATTRIBUTES.put(FormWidget.FormPart.FORM, NodeAttribute.FORM_CLASS);
    FORM_PART_CLASS_ATTRIBUTES.put(FormWidget.FormPart.TOP_LEVEL_FORM, NodeAttribute.TOP_LEVEL_FORM_CLASS);
    FORM_PART_CLASS_ATTRIBUTES.put(FormWidget.FormPart.NESTED_FORM, NodeAttribute.NESTED_CLASS);
    FORM_PART_CLASS_ATTRIBUTES.put(FormWidget.FormPart.NESTED_FORM, NodeAttribute.NESTED_FORM_CLASS);
    FORM_PART_CLASS_ATTRIBUTES.put(FormWidget.FormPart.FIELD_CELL, NodeAttribute.FIELD_CELL_CLASS);
    FORM_PART_CLASS_ATTRIBUTES.put(FormWidget.FormPart.FIELD_CELL, NodeAttribute.FORM_TABLE_CLASS);
    FORM_PART_CLASS_ATTRIBUTES.put(FormWidget.FormPart.FIELD_CELL, NodeAttribute.FORM_CELL_CLASS);
    FORM_PART_CLASS_ATTRIBUTES.put(FormWidget.FormPart.FIELD_CELL, NodeAttribute.CELL_CLASS);
    FORM_PART_CLASS_ATTRIBUTES.put(FormWidget.FormPart.PROMPT_CELL, NodeAttribute.PROMPT_CLASS);
    FORM_PART_CLASS_ATTRIBUTES.put(FormWidget.FormPart.PROMPT_CELL, NodeAttribute.FORM_CELL_CLASS);
    FORM_PART_CLASS_ATTRIBUTES.put(FormWidget.FormPart.PROMPT_CELL, NodeAttribute.CELL_CLASS);

    FORM_PART_STYLE_ATTRIBUTES.put(FormWidget.FormPart.FORM, NodeAttribute.STYLE);
    FORM_PART_STYLE_ATTRIBUTES.put(FormWidget.FormPart.FORM, NodeAttribute.FORM_STYLE);
    FORM_PART_STYLE_ATTRIBUTES.put(FormWidget.FormPart.TOP_LEVEL_FORM, NodeAttribute.TOP_LEVEL_FORM_STYLE);
    FORM_PART_STYLE_ATTRIBUTES.put(FormWidget.FormPart.NESTED_FORM, NodeAttribute.NESTED_STYLE);
    FORM_PART_STYLE_ATTRIBUTES.put(FormWidget.FormPart.NESTED_FORM, NodeAttribute.NESTED_FORM_STYLE);
    FORM_PART_STYLE_ATTRIBUTES.put(FormWidget.FormPart.FIELD_CELL, NodeAttribute.FIELD_CELL_STYLE);
    FORM_PART_STYLE_ATTRIBUTES.put(FormWidget.FormPart.FIELD_CELL, NodeAttribute.FORM_TABLE_STYLE);
    FORM_PART_STYLE_ATTRIBUTES.put(FormWidget.FormPart.FIELD_CELL, NodeAttribute.FORM_CELL_STYLE);
    FORM_PART_STYLE_ATTRIBUTES.put(FormWidget.FormPart.FIELD_CELL, NodeAttribute.CELL_STYLE);
    FORM_PART_STYLE_ATTRIBUTES.put(FormWidget.FormPart.PROMPT_CELL, NodeAttribute.PROMPT_STYLE);
    FORM_PART_STYLE_ATTRIBUTES.put(FormWidget.FormPart.PROMPT_CELL, NodeAttribute.FORM_CELL_STYLE);
    FORM_PART_STYLE_ATTRIBUTES.put(FormWidget.FormPart.PROMPT_CELL, NodeAttribute.CELL_STYLE);
  }

  public static final WidgetBuilder<PDFSerialiser, EvaluatedNodeInfo> getInstance() {
    return INSTANCE;
  }
}
