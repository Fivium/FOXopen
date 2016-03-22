package net.foxopen.fox.entrypoint.auth.saml;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConBindMap;
import net.foxopen.fox.database.UConStatementResult;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.sql.SQLManager;
import net.foxopen.fox.thread.RequestContext;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Get config options from the database for SAML
 */
public class SamlConfig {
  private static final String GET_SAML_CONFIG_FILENAME = "GetSamlConfig.sql";
  private static final String GET_SAML_CERTIFICATES_FILENAME = "GetSamlCertificates.sql";


  private final String mCertificateMnem;
  private final Map<String, String> mAttributeMap = new HashMap<>();
  private final List<SamlCertificate> mCertificateList = new ArrayList<>();
  private final String mAuthDomain;
  private final String mAuthScheme;

  /**
   * Call out to the database to find out the appropriate SAML config for a given request
   *
   * @param pRequestContext
   */
  public SamlConfig(RequestContext pRequestContext) {
    CertificateFactory lCertFactory;
    try {
      lCertFactory = CertificateFactory.getInstance("X.509");
    }
    catch (CertificateException e) {
      throw new ExInternal("Failed to create the certificate factory required for SAML auth", e);
    }

    UConBindMap lBindMap = new UConBindMap()
      .defineBind(":request_uri", getFullRequestURI(pRequestContext))
      .defineBind(":app_mnem", pRequestContext.getRequestAppMnem());
    UCon lUCon = pRequestContext.getContextUCon().getUCon("Get SAML Config");
    try {
      DOM lConfigDOM = lUCon.queryScalarDOM(SQLManager.instance().getStatement(GET_SAML_CONFIG_FILENAME, getClass()), lBindMap);

      if (lConfigDOM == null) {
        throw new ExInternal("Entry theme has auth type of SAML and a SAML Response was found, but no SAML config could be found for request URI '" + pRequestContext.getFoxRequest().getRequestURI() + "' and app mnem '" + pRequestContext.getRequestAppMnem() + "'");
      }

      mCertificateMnem = lConfigDOM.get1S("/saml-config/certificate-mnem");

      mAuthDomain = XFUtil.nvl(lConfigDOM.get1SNoEx("/saml-config/auth-domain"), "SAML");
      mAuthScheme = XFUtil.nvl(lConfigDOM.get1SNoEx("/saml-config/auth-scheme"), "SAML");

      for (DOM lAttributeDefinition : lConfigDOM.getUL("/saml-config/attribute-list/*")){
        mAttributeMap.put(lAttributeDefinition.getName(), lAttributeDefinition.value());
      }

      // Get the certificates
      UConBindMap lCertificateListBindMap = new UConBindMap().defineBind(":mnem", mCertificateMnem);
      List<UConStatementResult> lUConStatementResults = lUCon.queryMultipleRows(SQLManager.instance().getStatement(GET_SAML_CERTIFICATES_FILENAME, getClass()), lCertificateListBindMap);
      for (UConStatementResult lResult : lUConStatementResults) {
        InputStream lCertificateStream = lResult.getBlob("CERTIFICATE").getBinaryStream();

        Certificate lCertificate = lCertFactory.generateCertificate(lCertificateStream);

        mCertificateList.add(new SamlCertificate(lResult.getLong("ID"), lResult.getString("DESCRIPTION"), lCertificate));
        lCertificateStream.close();
      }
    }
    catch (ExDB | ExTooMany | ExTooFew | CertificateException | SQLException | IOException pEx) {
      throw new ExInternal("Failed to get SAML config for request URI '" + pRequestContext.getFoxRequest().getRequestURI() + "' and app mnem '" + pRequestContext.getRequestAppMnem() + "'", pEx);
    }
    finally {
      pRequestContext.getContextUCon().returnUCon(lUCon, "Get SAML Config");
    }
  }

  /**
   * Get the full request URI
   *
   * @param pRequestContext Context to build full request from
   * @return Full Request URI
   */
  private String getFullRequestURI(RequestContext pRequestContext) {
    String lScheme = pRequestContext.getFoxRequest().getHttpRequest().getScheme();
    int lServerPort = pRequestContext.getFoxRequest().getHttpRequest().getServerPort();

    //Only add a port suffix if it's not the standard port for the current scheme
    String lPort = "";
    if(!(("http".equals(lScheme) && lServerPort == 80) || ("https".equals(lScheme) && lServerPort == 443))) {
      lPort = ":" + lServerPort;
    }

    return lScheme + "://" + pRequestContext.getFoxRequest().getHttpRequest().getServerName() + lPort + pRequestContext.getFoxRequest().getRequestURI();
  }

  public String getCertificateMnem() {
    return mCertificateMnem;
  }

  public Map<String, String> getAttributeMap() {
    return Collections.unmodifiableMap(mAttributeMap);
  }

  public List<SamlCertificate> getCertificateList() {
    return Collections.unmodifiableList(mCertificateList);
  }

  public String getAuthDomain() {
    return mAuthDomain;
  }

  public String getAuthScheme() {
    return mAuthScheme;
  }

}
