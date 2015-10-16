package net.foxopen.fox.module.serialiser.widgets.pdf;


import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;

public class TickboxWidgetBuilder {
  private static final WidgetBuilder<PDFSerialiser, EvaluatedNodeInfo> INSTANCE = new MultiOptionSelectWidget("\u2611", "\u2610");

  public static final WidgetBuilder<PDFSerialiser, EvaluatedNodeInfo> getInstance() {
    return INSTANCE;
  }
}
