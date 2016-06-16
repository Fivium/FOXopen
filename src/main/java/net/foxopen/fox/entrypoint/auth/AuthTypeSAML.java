package net.foxopen.fox.entrypoint.auth;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.auth.AuthUtil;
import net.foxopen.fox.auth.AuthenticationContext;
import net.foxopen.fox.auth.SSOAuthenticatedInfo;
import net.foxopen.fox.auth.StandardAuthenticationContext;
import net.foxopen.fox.auth.loginbehaviours.SSOLoginBehaviour;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.entrypoint.auth.saml.SamlCertificate;
import net.foxopen.fox.entrypoint.auth.saml.SamlConfig;
import net.foxopen.fox.entrypoint.servlets.FoxMainServlet;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilder;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExSessionTimeout;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.track.Track;
import org.apache.commons.codec.binary.Base64;
import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.Response;
import org.opensaml.security.SAMLSignatureProfileValidator;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.security.credential.BasicCredential;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureValidator;
import org.opensaml.xml.validation.ValidationException;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Look for a SAMLResponse parameter before entry, if one is found attempt to parse it into a SAML Response object and
 * validate it before attempting to log in a user using the attributes defined in the SAML Response.
 */
public class AuthTypeSAML implements AuthType {

  private static AuthType INSTANCE = new AuthTypeSAML();
  public static AuthType getInstance() {
    return INSTANCE;
  }
  private AuthTypeSAML() {
  }

  @Override
  public AuthenticationContext processBeforeEntry(RequestContext pRequestContext) throws ExSessionTimeout {
    StandardAuthenticationContext lSAC = new StandardAuthenticationContext(pRequestContext);

    String lSamlResponseParameter = pRequestContext.getFoxRequest().getParameter("SAMLResponse");
    if (!XFUtil.isNull(lSamlResponseParameter)) {
      Track.pushInfo("AuthTypeSAML");
      try {
        Track.logInfoText("SAMLResponse", lSamlResponseParameter);

        SamlConfig lSamlConfig = new SamlConfig(pRequestContext);

        // Get parameter and decode it
        String lDecodedSamlResponse = new String(Base64.decodeBase64(lSamlResponseParameter));

        Response lSamlResponse;
        try {
          // Turn SAML parameter string into a DOM
          // NOTE: This does not use the DOM.createDocumentFromXMLString("").convertToW3CDocument() as the resulting document does not unmarshall due to namespace errors
          DocumentBuilderFactory lBuilderFactory = DocumentBuilderFactory.newInstance();
          lBuilderFactory.setNamespaceAware(true);
          DocumentBuilder lBuilder = lBuilderFactory.newDocumentBuilder();
          Element lDocumentElement = lBuilder.parse(new ByteArrayInputStream(lDecodedSamlResponse.getBytes())).getDocumentElement();

          // Turn DOM into a SAML object
          UnmarshallerFactory lUMF = Configuration.getUnmarshallerFactory();
          Unmarshaller lUMr = lUMF.getUnmarshaller(lDocumentElement);
          XMLObject lUM = lUMr.unmarshall(lDocumentElement);
          if (!"Response".equals(lUM.getElementQName().getLocalPart())) {
            throw new ExInternal("Expected a SAML response protocol message, found: " + lUM.getElementQName().getLocalPart());
          }
          lSamlResponse = (Response)lUM;
        }
        catch (UnmarshallingException | SAXException | ParserConfigurationException | IOException e) {
          throw new ExInternal("SAMLResponse parameter contained data that could not be turned into a W3C DOM and then cast to a SAML Response object", e);
        }

        // Check the response validates against any of the certificates pointed to by the config
        boolean lValid = false;
        CERT_CHECK:
        for (SamlCertificate lCertificate : lSamlConfig.getCertificateList()) {
          Track.pushDebug("Certificate", "saml_certificates.id = " + lCertificate.getID().toString());
          Track.logDebugText("CertificateDescription", lCertificate.getDescription());
          try {
            lValid = isValidSAMLResponse(pRequestContext, lSamlResponse, lCertificate);

            // Break on the first valid certificate
            if (lValid) {
              break CERT_CHECK;
            }
          }
          finally {
            Track.pop("Certificate");
          }
        }

        // Check the overall validity and either track a login fail or create a SSO Login Behaviour at attempt to login using it
        if (!lValid) {
          Track.alert("SAML Login failed as the response was never validated successfully");
        }
        else {
          // Create an Authenticated Info object with data from the assertions
          SSOAuthenticatedInfo lSSOAuthenticatedInfo = createSSOAuthenticatedInfo(lSamlConfig, lSamlResponse);

          // Add the request URI into the AuthInfo DOM so securemgr.authentication.session_create_sso() can use it if needed
          DOM lAuthInfoDOM = lSSOAuthenticatedInfo.getDOM();
          RequestURIBuilder lURIBuilder = pRequestContext.createURIBuilder();
          lAuthInfoDOM.getCreate1ENoCardinalityEx("REQUEST_URI").setText(lURIBuilder.convertToAbsoluteURL(lURIBuilder.buildServletURI(FoxMainServlet.SERVLET_PATH) + pRequestContext.getFoxRequest().getRequestURI()));

          // Attempt login
          SSOLoginBehaviour lSSOLoginBehaviour = new SSOLoginBehaviour(lSSOAuthenticatedInfo.getLoginId(), AuthUtil.getClientInfoNVP(pRequestContext.getFoxRequest()), lSamlConfig.getAuthDomain(), lSamlConfig.getAuthScheme(), lAuthInfoDOM);
          lSAC.login(pRequestContext, lSSOLoginBehaviour);
        }
      }
      finally {
        Track.pop("AuthTypeSAML");
      }

    }

    return lSAC;
  }

  /**
   * Get the assertion from a SAML response
   *
   * @param pSamlResponse SAMl Response with 1 Assertion object in it
   * @return SAML Assertion object from pSamlResponse
   */
  private Assertion getResponseAssertion(Response pSamlResponse) {
    List lAssertions = pSamlResponse.getAssertions();

    if (lAssertions.size() != 1) {
      throw new ExInternal("Expected 1 assertion in the SAML response, found: " + lAssertions.size());
    }

    return (Assertion)lAssertions.get(0);
  }

  /**
   * Validate a SAML response object against a given certificate as well as checking the assertion was received during a
   * valid time period.
   *
   * @param pRequestContext Request context for the page churn
   * @param pSamlResponse SAMl Response to validate
   * @param pCertificate SamlCertificate containing a X.509 certificate to validate the SAML Response and Assertion signatures against
   * @return True if the saml response validates, false otherwise
   */
  private boolean isValidSAMLResponse(RequestContext pRequestContext, Response pSamlResponse, SamlCertificate pCertificate)  {
    // Turn the certificate into a credential we can use to validate with
    BasicCredential pubCredential = new BasicCredential();
    pubCredential.setPublicKey(pCertificate.getCertificate().getPublicKey());

    // Create validators that check the signature meets the SAML signature requirements and validates against the certificate
    SAMLSignatureProfileValidator lSAMLSignatureProfileValidator = new SAMLSignatureProfileValidator();
    SignatureValidator lSignatureValidator = new SignatureValidator(pubCredential);

    // Check to see if the response itself is signed, if so, validate it
    boolean lValidResponseSignature = true;
    try {
      Signature lResponseSignature = pSamlResponse.getSignature();
      if (lResponseSignature != null) {
        lSAMLSignatureProfileValidator.validate(lResponseSignature);
        lSignatureValidator.validate(lResponseSignature);
      }
    }
    catch (ValidationException e) {
      lValidResponseSignature = false;

      Track.pushAlert("Validation-Fail", "SAML response signature failed to validate");
      try {
        Track.logAlertText("ValidationFailureStack", XFUtil.getJavaStackTraceInfo(e));
      }
      finally {
        Track.pop("Validation-Fail");
      }
    }

    // Then get the assertion
    Assertion assertion = getResponseAssertion(pSamlResponse);

    // Test the timing of the assertion is withing the boundaries it supplies
    boolean lValidConditions = true;
    DateTime lNotOnOrAfter = assertion.getConditions().getNotOnOrAfter();
    if (lNotOnOrAfter != null && lNotOnOrAfter.isBeforeNow()) {
      lValidConditions = false;
      Track.alert("Validation-Fail", "Time limit is before now, fail SAML validation Time: "+ new DateTime().toString() +" Expiry: " + lNotOnOrAfter);
    }
    DateTime lNotBefore = assertion.getConditions().getNotBefore();
    if (lNotBefore != null && lNotBefore.isAfterNow()) {
      lValidConditions = false;
      Track.alert("Validation-Fail", "Time limit is before now, fail SAML validation Time: "+ new DateTime().toString() +" Expiry: " + lNotOnOrAfter);
    }

    // Check to see if the assertion is signed, if so, validate it
    boolean lValidAssertionSignature = true;
    try {
      Signature lAssertionSignature = assertion.getSignature();
      if (lAssertionSignature != null) {
        lSAMLSignatureProfileValidator.validate(lAssertionSignature);
        lSignatureValidator.validate(lAssertionSignature);
      }
    }
    catch (ValidationException e) {
      lValidAssertionSignature = false;

      Track.pushAlert("Validation-Fail", "SAML assertion signature failed to validate");
      try {
        Track.logAlertText("ValidationFailureStack", XFUtil.getJavaStackTraceInfo(e));
      }
      finally {
        Track.pop("Validation-Fail");
      }
    }

    // Summarise validity
    boolean lValid = lValidResponseSignature && lValidConditions && lValidAssertionSignature;

    if (lValid) {
      Track.info("Validation-Pass", "The SAML response validates against this certificate and time restrictions");
    }

    // Update the certificates table with last success/fail times
    if (!(lValidResponseSignature && lValidAssertionSignature)) {
      // Update fail time
      pCertificate.updateFailureStatus(pRequestContext);
    }
    else {
      // Update success time
      pCertificate.updateSuccessStatus(pRequestContext);
    }

    return lValid;
  }

  /**
   * Create a map of assertion attributes using their names and the text content from the attributes.
   *
   * @param pSamlResponse Response with one assertion in it containing 0 or more attributes
   * @return Map of attribute names to text content
   */
  private Map<String, String> getAssertionAttributes(Response pSamlResponse) {
    List<AttributeStatement> lAttributeStatements = getResponseAssertion(pSamlResponse).getAttributeStatements();
    if (lAttributeStatements.size() != 1) {
      throw new ExInternal("Expected 1 attribute statement in the SAML assertion, found: " + lAttributeStatements.size());
    }

    List<Attribute> lAttributesList = lAttributeStatements.get(0).getAttributes();
    Map<String, String> lAttributesMap = new HashMap<>(lAttributesList.size());
    for (Attribute lAttribute : lAttributesList) {
      List<XMLObject> lAttributeValues = lAttribute.getAttributeValues();
      if (lAttributeStatements.size() != 1) {
        throw new ExInternal("Expected 1 attribute value in the SAML assertion for attribute '" + lAttribute.getName() + "', found: " + lAttributeValues.size());
      }
      XMLObject lXMLObject = lAttributeValues.get(0);

      // This could deserialise lXMLObject into a typed attribute but all attribute values we're interested in should just have plain text content
      lAttributesMap.put(lAttribute.getName(), lXMLObject.getDOM().getTextContent());
    }

    return Collections.unmodifiableMap(lAttributesMap);
  }

  /**
   * Create an AuthenticatedInfo object to use for logging in with data taken from the SAML Response attributes
   *
   * @param pSamlConfig Config object mapping SAML Assertion Attributes to Authenticated Info data
   * @param pSamlResponse SAML Response object containing one Assertion with attributes that should match the values defined in the SAML config
   * @return SSO Authenticated Info object with all the values needed to login to FOX
   */
  private SSOAuthenticatedInfo createSSOAuthenticatedInfo(SamlConfig pSamlConfig, Response pSamlResponse) {
    // Read assertion attributes
    Map<String, String> lAttributesMap = getAssertionAttributes(pSamlResponse);

    // Map assertion attributes to Auth Info required attributes as specified in the config object
    String lUniqueID, lLoginID, lForename, lSurname, lPrimaryEmail;

    if (!XFUtil.isNull(pSamlConfig.getAttributeMap().get("unique-id"))) {
      lUniqueID = lAttributesMap.get(pSamlConfig.getAttributeMap().get("unique-id"));
    }
    else {
      lUniqueID = getResponseAssertion(pSamlResponse).getSubject().getNameID().getValue();
    }

    lLoginID = lAttributesMap.get(pSamlConfig.getAttributeMap().get("login-id"));
    lForename = lAttributesMap.get(pSamlConfig.getAttributeMap().get("forename"));
    lSurname = lAttributesMap.get(pSamlConfig.getAttributeMap().get("surname"));
    lPrimaryEmail = lAttributesMap.get(pSamlConfig.getAttributeMap().get("email-address"));

    // Check all the required attributes are there
    if ( XFUtil.isNull(lUniqueID)
      || XFUtil.isNull(lLoginID)
      || XFUtil.isNull(lForename)
      || XFUtil.isNull(lSurname)
      || XFUtil.isNull(lPrimaryEmail)) {
      throw new ExInternal("Unable to map all SAML config attributes (" + pSamlConfig.getAttributeMap().toString() + ") to the attributes in the SAML Response (" + lAttributesMap.toString() + ")");
    }

    return new SSOAuthenticatedInfo(lUniqueID, lLoginID, lForename, lSurname, lPrimaryEmail);
  }
}
