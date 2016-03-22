SELECT
  sc.id
, sc.description
, sc.certificate
FROM ${schema.auth}.saml_certificates sc
WHERE sc.mnem = :mnem
ORDER BY sc.preference ASC