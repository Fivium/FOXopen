package net.foxopen.fox.module.serialiser.layout.methods;

import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.serialiser.OutputSerialiser;
import net.foxopen.fox.module.serialiser.layout.LayoutResult;


public interface LayoutMethod {
  public LayoutResult doLayout(int pColumLimit, OutputSerialiser pSerialiser, EvaluatedNodeInfo pEvalNodeInfo);
}
