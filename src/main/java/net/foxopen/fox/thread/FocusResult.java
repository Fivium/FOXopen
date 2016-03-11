package net.foxopen.fox.thread;

import net.foxopen.fox.command.XDoResult;

/**
 * Evaluated data instructing an OutputGenerator to focus on a given element. See FocusCommand.
 */
public interface FocusResult
extends XDoResult {
  FocusType getFocusType();
}
