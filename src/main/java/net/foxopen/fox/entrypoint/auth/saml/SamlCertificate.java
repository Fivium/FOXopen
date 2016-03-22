package net.foxopen.fox.entrypoint.auth.saml;

import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConBindMap;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.sql.SQLManager;
import net.foxopen.fox.thread.RequestContext;

import java.security.cert.Certificate;

/**
 *
 */
public class SamlCertificate {
  private static final String UPDATE_SAML_CERTIFICATE_STATUS_FILENAME = "UpdateSamlCertificateStatus.sql";

  private final Long mID;
  private final String mDescription;
  private final Certificate mCertificate;

  public SamlCertificate(Long pID, String pDescription, Certificate pCertificate) {
    mID = pID;
    mDescription = pDescription;
    mCertificate = pCertificate;
  }

  public Long getID() {
    return mID;
  }

  public String getDescription() {
    return mDescription;
  }

  public Certificate getCertificate() {
    return mCertificate;
  }

  public void updateSuccessStatus(RequestContext pRequestContext) {
    updateStatus(pRequestContext, true);
  }

  public void updateFailureStatus(RequestContext pRequestContext) {
    updateStatus(pRequestContext, false);
  }

  private void updateStatus(RequestContext pRequestContext, boolean pValid) {
    UConBindMap lBindMap = new UConBindMap()
      .defineBind(":certificate_id", mID)
      .defineBind(":valid", (pValid ? "success" : "failure"));
    UCon lUCon = pRequestContext.getContextUCon().getUCon("Update SAML Cert status");
    try {
      lUCon.executeAPI(SQLManager.instance().getStatement(UPDATE_SAML_CERTIFICATE_STATUS_FILENAME, getClass()), lBindMap);
    }
    catch (ExDB pEx) {
      throw new ExInternal("Failed to update SAML certificate status for certificate.id=" + mID + "", pEx);
    }
    finally {
      pRequestContext.getContextUCon().returnUCon(lUCon, "Update SAML Cert status");
    }
  }
}