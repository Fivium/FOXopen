package net.foxopen.fox.command.builtin;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.FoxResponseCHAR;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.cache.BuiltInCacheDefinition;
import net.foxopen.fox.cache.CacheManager;
import net.foxopen.fox.cache.FoxCache;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConBindMap;
import net.foxopen.fox.database.UConStatementResult;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.logging.FoxLogger;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.sql.SQLManager;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.ResponseOverride;
import net.foxopen.fox.util.RandomString;
import org.apache.commons.codec.binary.Base64;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.signature.XMLSignature;
import org.joda.time.DateTime;
import org.opensaml.common.SAMLVersion;
import org.opensaml.common.SignableSAMLObject;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.Audience;
import org.opensaml.saml2.core.AudienceRestriction;
import org.opensaml.saml2.core.AuthnContext;
import org.opensaml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml2.core.AuthnStatement;
import org.opensaml.saml2.core.Conditions;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.NameIDType;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.Status;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.core.Subject;
import org.opensaml.saml2.core.SubjectConfirmation;
import org.opensaml.saml2.core.SubjectConfirmationData;
import org.opensaml.saml2.core.impl.AssertionBuilder;
import org.opensaml.saml2.core.impl.AttributeBuilder;
import org.opensaml.saml2.core.impl.AttributeStatementBuilder;
import org.opensaml.saml2.core.impl.AudienceBuilder;
import org.opensaml.saml2.core.impl.AudienceRestrictionBuilder;
import org.opensaml.saml2.core.impl.AuthnContextBuilder;
import org.opensaml.saml2.core.impl.AuthnContextClassRefBuilder;
import org.opensaml.saml2.core.impl.AuthnStatementBuilder;
import org.opensaml.saml2.core.impl.ConditionsBuilder;
import org.opensaml.saml2.core.impl.IssuerBuilder;
import org.opensaml.saml2.core.impl.StatusBuilder;
import org.opensaml.saml2.core.impl.StatusCodeBuilder;
import org.opensaml.saml2.core.impl.SubjectBuilder;
import org.opensaml.saml2.core.impl.SubjectConfirmationBuilder;
import org.opensaml.saml2.core.impl.SubjectConfirmationDataBuilder;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.XMLObjectBuilder;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallerFactory;
import org.opensaml.xml.schema.XSString;
import org.opensaml.xml.security.x509.BasicX509Credential;
import org.opensaml.xml.signature.KeyInfo;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.Signer;
import org.opensaml.xml.signature.X509Certificate;
import org.opensaml.xml.signature.X509Data;
import org.w3c.dom.Element;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import javax.xml.namespace.QName;
import java.io.StringWriter;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SAMLResponseCommand extends BuiltInCommand {
  private static final MustacheFactory MUSTACHE_FACTORY = new DefaultMustacheFactory("net/foxopen/fox/command/builtin/");
  private static final String GET_DIGITAL_CERTIFICATE_FILES = "GetDigitalCertificateFiles.sql";

  private static final String RESPONSE_ID_PREFIX = "r";
  private static final String ASSERTION_ID_PREFIX = "a";

  private final String mRelayStateXPath;
  private final String mRecipientXPath;
  private final String mAudienceURIXPath;
  private final String mTimeoutXPath;
  private final String mAssertionAttributesXPath;
  private final String mDigitalCertificateNameXPath;
  private final String mSignResponseXPath;
  private final String mSignAssertionXPath;

  /**
   * Constructs and initialises a command from the XML element
   * specified.
   *
   * @param pCommandDOM DOM of the command instance in the module
   */
  public SAMLResponseCommand(DOM pCommandDOM) throws ExDoSyntax {
    super(pCommandDOM);
    mRelayStateXPath = getAttribute("relay-state");

    mRecipientXPath = getAttribute("recipient");
    if(XFUtil.isNull(mRecipientXPath)) {
      throw new ExDoSyntax("The SAML Response command must have a recipient XPath defined");
    }
    mAudienceURIXPath = getAttribute("audience");
    if(XFUtil.isNull(mAudienceURIXPath)) {
      throw new ExDoSyntax("The SAML Response command must have an audience XPath defined");
    }

    mTimeoutXPath = getAttribute("timeout");

    mDigitalCertificateNameXPath = getAttribute("digital-certificate-name");
    mSignResponseXPath = getAttribute("sign-response");
    mSignAssertionXPath = getAttribute("sign-assertion");

    mAssertionAttributesXPath = getAttribute("assertion-attributes");
    if(XFUtil.isNull(mAssertionAttributesXPath)) {
      throw new ExDoSyntax("The SAML Response command must have an assertion-attributes XPath defined");
    }
  }

  /**
   * Run the fm:saml-response command
   *
   * @param pRequestContext Request context
   * @return
   */
  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {
    // Resolve basic attributes to variables from XPaths
    ContextUElem lContextUElem = pRequestContext.getContextUElem();
    String lRelayState = null;
    String lRecipient = null;
    String lAudienceURI = null;
    Integer lTimeoutMS = (int) TimeUnit.MINUTES.toMillis(1);
    String lDigitalCertificateName = null;
    boolean lSignResponse = false;
    boolean lSignAssertion = false;
    Map<String, String> lAssertionAttributes = new HashMap<>();
    try {
      if (!XFUtil.isNull(mRelayStateXPath)) {
        lRelayState = lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), mRelayStateXPath);
      }

      if (!XFUtil.isNull(mRecipientXPath)) {
        lRecipient = lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), mRecipientXPath);
      }

      if (!XFUtil.isNull(mAudienceURIXPath)) {
        lAudienceURI =  lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), mAudienceURIXPath);
      }

      if (!XFUtil.isNull(mTimeoutXPath)) {
        lTimeoutMS = Integer.parseInt(lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), mTimeoutXPath));
      }

      if (!XFUtil.isNull(mDigitalCertificateNameXPath)) {
        lDigitalCertificateName = lContextUElem.extendedStringOrXPathString(lContextUElem.attachDOM(), mDigitalCertificateNameXPath);

        if (!XFUtil.isNull(mSignResponseXPath)) {
          lSignResponse = lContextUElem.extendedXPathBoolean(lContextUElem.attachDOM(), mSignResponseXPath);
        }

        if (!XFUtil.isNull(mSignAssertionXPath)) {
          lSignAssertion = lContextUElem.extendedXPathBoolean(lContextUElem.attachDOM(), mSignAssertionXPath);
        }
      }
      else if (!XFUtil.isNull(mSignResponseXPath) || !XFUtil.isNull(mSignAssertionXPath)) {
        throw new ExInternal("Cannot sign the SAML Response or Assertion unless a digital-certificate-name is specified");
      }

      DOM lAssertionAttributesDOM = lContextUElem.extendedXPath1E(mAssertionAttributesXPath, false);
      for (DOM lAttribute : lAssertionAttributesDOM.getChildElements()) {
        lAssertionAttributes.put(lAttribute.getName(), lAttribute.value());
      }
    }
    catch (ExActionFailed | ExCardinality ex) {
      throw new ExInternal("Failed to parse XPath arguments for fm:saml-response command", ex);
    }


    // Build the SAML Response
    Response lResponse = buildResponse(pRequestContext, lTimeoutMS, lRecipient, lAudienceURI, lAssertionAttributes, lSignResponse, lSignAssertion, lDigitalCertificateName);

    byte[] lBase64EncodedResponse = Base64.encodeBase64(marshall(lResponse).getBytes());

    // Put the base64 result into a self posting form to the recipient
    StringWriter lStringWriter = new StringWriter(lBase64EncodedResponse.length + 1000);
    Map<String, String> lTemplateMap = new HashMap<>();
    lTemplateMap.put("RecipientURL", lRecipient);
    lTemplateMap.put("Base64SamlResponse", new String(lBase64EncodedResponse));
    if (!XFUtil.isNull(lRelayState)) {
      lTemplateMap.put("RelayState", lRelayState);
    }

    Mustache lMustache = MUSTACHE_FACTORY.compile("SamlResponsePage.mustache");
    lMustache.execute(lStringWriter, lTemplateMap);

    //Set the override response as an XDoResult to be picked up before HTML gen
    FoxResponseCHAR lHTMLResponse = new FoxResponseCHAR("text/html; charset=UTF-8", lStringWriter.getBuffer(), -1);

    pRequestContext.addXDoResult(new ResponseOverride(lHTMLResponse));

    return XDoControlFlowContinue.instance();
  }

  /**
   * Marshall a SAML XML object into a W3C DOM and then into a String
   *
   * @param pXMLObject SAML Object to marshall
   * @return XML version of the SAML Object in string form
   */
  private String marshall(XMLObject pXMLObject) {
		try {
			MarshallerFactory lMarshallerFactory = org.opensaml.xml.Configuration.getMarshallerFactory();
			Marshaller lMarshaller = lMarshallerFactory.getMarshaller(pXMLObject);
			Element lElement = lMarshaller.marshall(pXMLObject);

			DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
			DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");
			LSSerializer writer = impl.createLSSerializer();
			return writer.writeToString(lElement);
		}
    catch (Exception e) {
			throw new ExInternal("Error Serializing the SAML Response", e);
		}
	}

  /**
   * Build the SAML Response Object
   *
   * @param pRequestContext
   * @param pTimeoutMS
   * @param pRecipient
   * @param pAudienceURI
   * @param pAssertionAttributes
   * @param pSignResponse
   * @param pSignAssertion
   * @param pDigitalCertificateName
   * @return SAML Response Object
   */
  private Response buildResponse(ActionRequestContext pRequestContext
    , Integer pTimeoutMS
    , String pRecipient
    , String pAudienceURI
    , Map<String, String> pAssertionAttributes
    , boolean pSignResponse
    , boolean pSignAssertion
    , String pDigitalCertificateName
  ) {
    Response lResponse = new org.opensaml.saml2.core.impl.ResponseBuilder().buildObject();
    lResponse.setIssuer(buildIssuer(pRequestContext.getFoxRequest().getHttpRequest().getRequestURL().toString()));
    lResponse.setID(RESPONSE_ID_PREFIX + RandomString.getString(40));
//    lResponse.setInResponseTo(authReqDTO.getId());
//    lResponse.setDestination(authReqDTO.getAssertionConsumerURL());
    lResponse.setStatus(buildStatus(StatusCode.SUCCESS_URI));
    lResponse.setVersion(SAMLVersion.VERSION_20);

    DateTime issueInstant = new DateTime();
    lResponse.setIssueInstant(issueInstant);


    DateTime lNotOnOrAfter = new DateTime(issueInstant.getMillis() + pTimeoutMS);
    Assertion assertion = buildSAMLAssertion(pRequestContext, lNotOnOrAfter, pRecipient, pAudienceURI, pAssertionAttributes, pSignAssertion, pDigitalCertificateName);
    lResponse.getAssertions().add(assertion);

    // Sign the SAML Response object
    if (pSignResponse) {
      signSAMLObject(lResponse, pRequestContext, pDigitalCertificateName);
    }

    return lResponse;
  }

  /**
   * Build a SAML Issuer object
   *
   * @param pIssuer URL of the SAML Response Issuer
   * @return SAML Issuer Object
   */
  private Issuer buildIssuer(String pIssuer) {
		Issuer issuer = new IssuerBuilder().buildObject();
		issuer.setValue(pIssuer);
		issuer.setFormat(NameIDType.ENTITY);
		return issuer;
	}

  /**
   * Build a SAML Status object
   *
   * @param pStatusCode Code the the SAML Status Object
   * @return SAML Status object
   */
  private Status buildStatus(String pStatusCode) {
    Status lStatus = new StatusBuilder().buildObject();

    // Set the status code
    StatusCode lStatusCode = new StatusCodeBuilder().buildObject();
    lStatusCode.setValue(pStatusCode);
    lStatus.setStatusCode(lStatusCode);

    return lStatus;
  }

  /**
   * Build SAML Assertion object
   *
   * @param pRequestContext
   * @param pNotOnOrAfter
   * @param pRecipient
   * @param pAudienceURI
   * @param pAssertionAttributes
   * @param pSignAssertion
   * @param pDigitalCertificateName
   * @return SAML Assertion Object
   */
  private Assertion buildSAMLAssertion(ActionRequestContext pRequestContext
    , DateTime pNotOnOrAfter
    , String pRecipient
    , String pAudienceURI
    , Map<String, String> pAssertionAttributes
    , boolean pSignAssertion
    , String pDigitalCertificateName
  ) {
    DateTime lCurrentTime = new DateTime();

    Assertion lAssertion = new AssertionBuilder().buildObject();
    lAssertion.setID(ASSERTION_ID_PREFIX + RandomString.getString(40));
    lAssertion.setVersion(SAMLVersion.VERSION_20);
    lAssertion.setIssuer(buildIssuer(pRequestContext.getFoxRequest().getHttpRequest().getRequestURL().toString()));
    lAssertion.setIssueInstant(lCurrentTime);

    Subject lSubject = new SubjectBuilder().buildObject();
    SubjectConfirmation lSubjectConfirmation = new SubjectConfirmationBuilder().buildObject();
    lSubjectConfirmation.setMethod(SubjectConfirmation.METHOD_BEARER);
    SubjectConfirmationData lSubjectConfirmationData = new SubjectConfirmationDataBuilder().buildObject();
    if (!XFUtil.isNull(pRecipient)) {
      lSubjectConfirmationData.setRecipient(pRecipient);
    }
    lSubjectConfirmationData.setNotOnOrAfter(pNotOnOrAfter);
    lSubjectConfirmation.setSubjectConfirmationData(lSubjectConfirmationData);
    lSubject.getSubjectConfirmations().add(lSubjectConfirmation);
    lAssertion.setSubject(lSubject);

    AuthnStatement lAuthnStatement = new AuthnStatementBuilder().buildObject();
    lAuthnStatement.setAuthnInstant(new DateTime());
    AuthnContext lAuthnContext = new AuthnContextBuilder().buildObject();
    AuthnContextClassRef lAuthnContextClassRef = new AuthnContextClassRefBuilder().buildObject();
    lAuthnContextClassRef.setAuthnContextClassRef(AuthnContext.PASSWORD_AUTHN_CTX);
    lAuthnContext.setAuthnContextClassRef(lAuthnContextClassRef);
    lAuthnStatement.setAuthnContext(lAuthnContext);
    lAssertion.getAuthnStatements().add(lAuthnStatement);

    if (pAssertionAttributes != null && pAssertionAttributes.size() > 0) {
      lAssertion.getAttributeStatements().add(buildAttributeStatement(pAssertionAttributes));
    }

    AudienceRestriction lAudienceRestriction = new AudienceRestrictionBuilder().buildObject();
    if (!XFUtil.isNull(pAudienceURI)) {
      Audience lAudience = new AudienceBuilder().buildObject();
      lAudience.setAudienceURI(pAudienceURI);
      lAudienceRestriction.getAudiences().add(lAudience);
    }

    Conditions lConditions = new ConditionsBuilder().buildObject();
    lConditions.setNotBefore(lCurrentTime);
    lConditions.setNotOnOrAfter(pNotOnOrAfter);
    lConditions.getAudienceRestrictions().add(lAudienceRestriction);
    lAssertion.setConditions(lConditions);

    if (pSignAssertion) {
      signSAMLObject(lAssertion, pRequestContext, pDigitalCertificateName);
    }

    return lAssertion;
  }

  /**
   * Sign a Signable SAML Object using a given certificate
   *
   * @param pSignableSAMLObject SAML Object to sign
   * @param pRequestContext Request context for database connection if it needs to retrieve digital certificate files
   * @param pDigitalCertificateName Name for digital certificate files
   * @return Signed pSignableSAMLObject
   */
  public SignableSAMLObject signSAMLObject(SignableSAMLObject pSignableSAMLObject, RequestContext pRequestContext, String pDigitalCertificateName) {
    try {
      BasicX509Credential lCredential = getSigningCredential(pRequestContext, pDigitalCertificateName);

      // Create the SAML Signature object
      Signature signature = (Signature) buildXMLObject(Signature.DEFAULT_ELEMENT_NAME);
      signature.setSigningCredential(lCredential);
      signature.setSignatureAlgorithm(XMLSignature.ALGO_ID_SIGNATURE_RSA);
      signature.setCanonicalizationAlgorithm(Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

      // Add the signing key info to the signature object
      try {
        KeyInfo keyInfo = (KeyInfo) buildXMLObject(KeyInfo.DEFAULT_ELEMENT_NAME);
        X509Data data = (X509Data) buildXMLObject(X509Data.DEFAULT_ELEMENT_NAME);
        X509Certificate cert = (X509Certificate) buildXMLObject(X509Certificate.DEFAULT_ELEMENT_NAME);
        String value = org.apache.xml.security.utils.Base64.encode(lCredential.getEntityCertificate().getEncoded());
        cert.setValue(value);
        data.getX509Certificates().add(cert);
        keyInfo.getX509Datas().add(data);
        signature.setKeyInfo(keyInfo);
      }
      catch (CertificateEncodingException e) {
        throw new ExInternal("Failed to encode the signing certificate", e);
      }

      // Add the signature to the SAML object
      pSignableSAMLObject.setSignature(signature);

      // Sign and marshall the result
      List<Signature> signatureList = new ArrayList<>();
      signatureList.add(signature);
      MarshallerFactory marshallerFactory = org.opensaml.xml.Configuration.getMarshallerFactory();
      Marshaller marshaller = marshallerFactory.getMarshaller(pSignableSAMLObject);

      marshaller.marshall(pSignableSAMLObject);

      org.apache.xml.security.Init.init();
      Signer.signObjects(signatureList);
      return pSignableSAMLObject;
    }
    catch (Exception e) {
      throw new ExInternal("Error while signing the SAML Response message.", e);
    }
  }

  /**
   * Get a credential suitable for digitally signing XML for a given digital certificate ID
   *
   * @param pRequestContext Request context to get a connection from in case it needs to go to the database
   * @param pDigitalCertificateName Name of the digital certificate files to use
   * @return X.509 Credential suitable for digitally signing XML
   */
  private BasicX509Credential getSigningCredential(RequestContext pRequestContext, String pDigitalCertificateName) {
    // Attempt to get the certificate from cache first
    FoxCache<String, BasicX509Credential> lFoxCache = CacheManager.getCache(BuiltInCacheDefinition.SAML_SIGNING_CERTS);
    BasicX509Credential lCachedCredential = lFoxCache.get(pDigitalCertificateName);
    if (lCachedCredential != null) {
      return lCachedCredential;
    }

    // If it wasn't in the cache go to the database to get the cert/private key and create it
    try {
      CertificateFactory lCertFactory;
      try {
        lCertFactory = CertificateFactory.getInstance("X.509");
      }
      catch (CertificateException e) {
        throw new ExInternal("Failed to create the certificate factory required for SAML auth", e);
      }

      BasicX509Credential lCredential = new BasicX509Credential();

      UConBindMap lBindMap = new UConBindMap().defineBind(":digital_cert_name", pDigitalCertificateName);
      UCon lUCon = pRequestContext.getContextUCon().getUCon("Get SAML Signing Certificate");
      try {
        UConStatementResult lUConStatementResult = lUCon.querySingleRow(SQLManager.instance().getStatement(GET_DIGITAL_CERTIFICATE_FILES, getClass()), lBindMap);
        Blob lCertificateFile = lUConStatementResult.getBlob("CERTIFICATE_FILE");
        Blob lPrivateKeyFile = lUConStatementResult.getBlob("PRIVATE_KEY_FILE");
        String lCertificateFileType = lUConStatementResult.getString("CERTIFICATE_FILE_TYPE");

        if (!"X509".equals(lCertificateFileType)) {
          throw new ExInternal("Digital certificates for SAML signing MUST be X509 type, given NAME=" + pDigitalCertificateName + ", found type=" + lCertificateFileType);
        }

        // Load the certificate
        Certificate lCertificate = lCertFactory.generateCertificate(lCertificateFile.getBinaryStream());
        lCredential.setEntityCertificate((java.security.cert.X509Certificate) lCertificate);

        // Load private key file
        byte[] lPrivateKeyBytes = XFUtil.toByteArray(lPrivateKeyFile.getBinaryStream(), 2048, -1);
        if (lPrivateKeyBytes.length == 0) {
          throw new IllegalArgumentException("Zero-length file found for certificate NAME=" + pDigitalCertificateName);
        }

        KeyFactory keyFactory;
        try {
          keyFactory = KeyFactory.getInstance("RSA");
          EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(lPrivateKeyBytes);
          lCredential.setPrivateKey(keyFactory.generatePrivate(privateKeySpec));
        }
        catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
          FoxLogger.getLogger().error("Failed to decode private key for certificate NAME=" + pDigitalCertificateName, e);
          throw new ExInternal(e.getMessage(), e);
        }

        lFoxCache.put(pDigitalCertificateName, lCredential);
        return lCredential;
      }
      catch (ExDB pEx) {
        throw new ExInternal("Failed to get a Digital certificates for SAML signing for certificate NAME=" + pDigitalCertificateName, pEx);
      }
      finally {
        pRequestContext.getContextUCon().returnUCon(lUCon, "Get SAML Signing Certificate");
      }
    }
    catch (Exception e) {
      throw new ExInternal("Error while signing the SAML Response message.", e);
    }
  }

  /**
   * Build attribute statement section for a SAML Assertion
   *
   * @param pAttributeMap Map of attribute keys to values (currently strings only)
   * @return SAML Attribute Statement object for inclusion in a SAML Assertion
   */
  private AttributeStatement buildAttributeStatement(Map<String, String> pAttributeMap) {
    AttributeStatement lAttributeStatement = null;
    if (pAttributeMap != null) {
      lAttributeStatement = new AttributeStatementBuilder().buildObject();

      for (Map.Entry<String, String> lAttributeEntry : pAttributeMap.entrySet()) {
        Attribute lAttribute = new AttributeBuilder().buildObject();
        lAttribute.setName(lAttributeEntry.getKey());

        // Currently just set all value as string type
        XSString lStringAttributeValue = (XSString) buildXMLObject(XSString.TYPE_NAME);
        lStringAttributeValue.setValue(lAttributeEntry.getValue());

        lAttribute.getAttributeValues().add(lStringAttributeValue);
        lAttributeStatement.getAttributes().add(lAttribute);
      }
    }
    return lAttributeStatement;
  }

  /**
	 * Builds SAML Objects for a given QName
	 *
	 * @param pQName Element QName for the SAML Object
	 * @return SAML Object
	 */
	private static XMLObject buildXMLObject(QName pQName) {
		XMLObjectBuilder lObjectBuilder = org.opensaml.xml.Configuration.getBuilderFactory().getBuilder(pQName);

		if (lObjectBuilder == null) {
			throw new ExInternal("Cannot get builder for QName: " + pQName);
		}

		return lObjectBuilder.buildObject(pQName.getNamespaceURI(), pQName.getLocalPart(), pQName.getPrefix());
	}











  @Override
  public boolean isCallTransition() {
    return false;
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new SAMLResponseCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Arrays.asList("saml-response");
    }
  }
}
