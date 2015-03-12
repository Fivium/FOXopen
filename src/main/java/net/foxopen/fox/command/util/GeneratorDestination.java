package net.foxopen.fox.command.util;

import net.foxopen.fox.thread.ActionRequestContext;

/**
 * Encapsulation of shared functionality for writing binary or character data from Command output to a common destination
 * (typically a download or storage location). A GeneratorDestination is determined from command markup (i.e. if the user
 * has specified a download file name or destination Storage Location name) and an instance is held on the parsed command
 * object. When the command is executed, it invokes one of the "generateToXXX" method and provides an XXXGenerator implementation
 * which actually performs the write. The GeneratorDestination handles any bootstrap or cleandown actions from the write.
 */
public interface GeneratorDestination {

  void generateToWriter(ActionRequestContext pRequestContext, WriterGenerator pGenerator);

  void generateToOutputStream(ActionRequestContext pRequestContext, OutputStreamGenerator pGenerator);

  void generateToDOM(ActionRequestContext pRequestContext, DOMGenerator pGenerator);

}
