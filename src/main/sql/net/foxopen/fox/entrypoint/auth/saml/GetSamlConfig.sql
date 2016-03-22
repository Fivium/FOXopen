SELECT
  ${schema.auth}.saml.get_saml_config(
    p_request_uri => :request_uri
  , p_app_mnem    => :app_mnem
  )
FROM dual
