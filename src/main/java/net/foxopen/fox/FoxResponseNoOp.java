package net.foxopen.fox;

/**
 * A FoxResponse which does nothing. Methods which return this object should ensure they manually send a response.
 */
public class FoxResponseNoOp
extends FoxResponse {

  @Override
  public void respond(FoxRequest pRequest) {
  }
}
