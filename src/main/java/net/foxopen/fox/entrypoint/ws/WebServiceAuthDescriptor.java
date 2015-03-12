package net.foxopen.fox.entrypoint.ws;

import com.google.common.collect.Sets;
import net.foxopen.fox.banghandler.InternalAuthLevel;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExInternal;

import java.util.Arrays;
import java.util.Collection;

/**
 * AuthDescriptor for a WebService, encapsulating the permitted auth types and whether dev access is allowed (i.e. if the
 * WebService should always be available on a dev system). This object will be interrogated by the WebService entry handler
 * to authenticate every web service request. <br><br>
 *
 * An empty collection of allowed auth types indicates that no authentication is required. Consumers requiring this behaviour
 * should use the provided static singleton. <br><br>
 *
 * If Internal access is allowed, the Descriptor should provide a minimum internal access level.
 */
public class WebServiceAuthDescriptor {

  /** Singleton instance of a "no auth required" descriptor. */
  public static final WebServiceAuthDescriptor NO_AUTHENTICATION_REQUIRED = new WebServiceAuthDescriptor(true);

  private final boolean mDevAccessAllowed;
  private final InternalAuthLevel mInternalAuthLevel;
  private final Collection<WebServiceAuthType> mAllowedAuthTypes;

  /**
   * Creates a WebServiceAuthDescriptor for non-internal access.
   * @param pDevAccessAllowed If true, access to the WebService is always allowed on development systems.
   * @param pAllowedAuthTypes 0 or more allowed auth types. If none are provided, no authentication will be required by
   *                          this descriptor. You cannot specify INTERNAL using this signature.
   */
  public WebServiceAuthDescriptor(boolean pDevAccessAllowed, WebServiceAuthType... pAllowedAuthTypes) {
    mDevAccessAllowed = pDevAccessAllowed;
    mAllowedAuthTypes = Sets.newEnumSet(Arrays.asList(pAllowedAuthTypes), WebServiceAuthType.class);

    if(mAllowedAuthTypes.contains(WebServiceAuthType.INTERNAL)) {
      throw new ExInternal("You must specify an internal auth level if internal auth is allowed");
    }
    else {
      mInternalAuthLevel = InternalAuthLevel.NONE;
    }
  }

  /**
   * Creates a WebServiceAuthDescriptor which allows internal access.
   * @param pDevAccessAllowed If true, access to the WebService is always allowed on development systems.
   * @param pInternalAuthLevel Internal auth level to enforce when authenticating internally. This cannot be "NONE".
   * @param pAllowedAuthTypes Allowed auth types, of which one should be INTERNAL. If internal access is not required
   *                          the other constructor should be used.
   */
  public WebServiceAuthDescriptor(boolean pDevAccessAllowed, InternalAuthLevel pInternalAuthLevel, WebServiceAuthType... pAllowedAuthTypes) {
    mDevAccessAllowed = pDevAccessAllowed;
    mInternalAuthLevel = pInternalAuthLevel;
    mAllowedAuthTypes = Sets.newEnumSet(Arrays.asList(pAllowedAuthTypes), WebServiceAuthType.class);

    if(pInternalAuthLevel.intValue() <= InternalAuthLevel.NONE.intValue()) {
      throw new ExInternal("If internal authentication is required, the internal auth level cannot be 'none'");
    }
  }

  /**
   * Gets the Internal auth level required by this descriptor. If internal auth is not required or allowed this will return
   * {@link InternalAuthLevel}.NONE.
   * @return
   */
  InternalAuthLevel getRequiredInternalAuthLevel() {
    return mInternalAuthLevel;
  }

  /**
   * Tests if this AuthDescriptor actually requires any authentication to be performed.
   * @return True if some form of authentication needs to be performed according to this descriptor.
   */
  boolean authenticationRequired() {
    return !(mAllowedAuthTypes.isEmpty() || (mDevAccessAllowed && FoxGlobals.getInstance().isDevelopment()));
  }

  /**
   * Tests if the given auth type is a permitted auth type according to this descriptor.
   * @param pAuthType
   * @return
   */
  boolean isAuthTypeAllowed(WebServiceAuthType pAuthType) {
    return mAllowedAuthTypes.contains(pAuthType);
  }

  public String toString() {
    return mAllowedAuthTypes.toString() + " (InternalAuthLevel=" + mInternalAuthLevel + ", DevAccess=" + mDevAccessAllowed + ")";
  }
}
