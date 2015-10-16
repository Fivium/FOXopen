package net.foxopen.fox.module.serialiser.widgets.pdf;


import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;

public class RadioWidgetBuilder {
  private static final WidgetBuilder<PDFSerialiser, EvaluatedNodeInfo> INSTANCE = new MultiOptionSelectWidget("\u25C9", "\u25CB");

  public static final WidgetBuilder<PDFSerialiser, EvaluatedNodeInfo> getInstance() {
    return INSTANCE;
  }
}
