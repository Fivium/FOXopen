SELECT
  dc.certificate_name
, dc.certificate_file
, dc.private_key_file
, dc.certificate_file_type
FROM decmgr.digital_certificates dc
WHERE dc.certificate_name = :digital_cert_name