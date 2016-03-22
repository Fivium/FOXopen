BEGIN
  UPDATE ${schema.auth}.saml_certificates
  SET last_success_datetime = SYSDATE
  WHERE id = :certificate_id
  AND :valid = 'success';

  UPDATE ${schema.auth}.saml_certificates
  SET last_failure_datetime = SYSDATE
  WHERE id = :certificate_id
  AND :valid = 'failure';

END;