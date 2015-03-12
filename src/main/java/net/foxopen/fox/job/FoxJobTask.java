package net.foxopen.fox.job;


/**
 * A single task/item of work which will be executed by a FoxJobPool.<br/><br>
 *
 * Implementors of this interface should ensure that any Throwables thrown by the executeTask method are properly contextualised
 * so meaningful error messages can be displayed in the engine status viewer. It is recommended that you have a wrapping
 * try block in your executeTask method which catches all Throwables and wraps them in a useful error message which explains what
 * the task was trying to do.
 */
public interface FoxJobTask {

  public TaskCompletionMessage executeTask();

}
