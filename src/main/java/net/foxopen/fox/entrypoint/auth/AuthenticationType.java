package net.foxopen.fox.entrypoint.auth;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.foxopen.fox.ex.ExInternal;


public enum AuthenticationType {
  FOX_COMMAND(AuthTypeFoxCommand.getInstance(), "fox-command", "portal"),
  HTTP_AUTH_BASIC(AuthTypeHttpAuthBasic.getInstance(), "http-auth-basic", "http"),
  HTTP_HEADER(AuthTypeHttpHeader.getInstance(), "http-header"),
  SAML(AuthTypeSAML.getInstance(), "saml");

  private static Map<String, AuthenticationType> AUTH_TYPE_LOOKUP_MAP = new HashMap<>(AuthenticationType.values().length);
  static {
    for (AuthenticationType lAuthenticationType : AuthenticationType.values()) {
      for (String lAlias : lAuthenticationType.mAliases) {
        AUTH_TYPE_LOOKUP_MAP.put(lAlias, lAuthenticationType);
      }
    }
  }

  private final Set<String> mAliases = new HashSet<>();
  private final transient AuthType mAuthType;

  private AuthenticationType(AuthType pAuthType, String... pAliases) {
    mAuthType = pAuthType;
    if (pAliases != null) {
      mAliases.addAll(Arrays.asList(pAliases));
    }
  }

  public static AuthenticationType fromString(String pAuthenticationTypeName) {
    AuthenticationType lAuthType = AUTH_TYPE_LOOKUP_MAP.get(pAuthenticationTypeName.toLowerCase());
    if (lAuthType == null) {
      throw new ExInternal("Unknown AuthenticationType asked for by name: " + pAuthenticationTypeName);
    }

    return lAuthType;
  }

  public AuthType getAuthType() {
    return mAuthType;
  }
}
