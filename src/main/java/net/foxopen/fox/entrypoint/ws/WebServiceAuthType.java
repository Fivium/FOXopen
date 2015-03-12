package net.foxopen.fox.entrypoint.ws;

/**
 * Authentication types which may be required by a WebService.
 */
public enum WebServiceAuthType {
  /** Token based auth (from the database security token). */
  TOKEN,
  /** !LOGIN as an admin or support user (specified on AuthDescriptor) */
  INTERNAL;
}
