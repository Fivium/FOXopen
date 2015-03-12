BEGIN
  :session_id_out := ${schema.auth}.authentication.session_create_sso(
    p_origin_client_info       => :client_info
  , p_application_display_name => :app_display_name
  , p_login_id                 => :login_id
  , p_pre_auth_xml             => :pre_auth_xml
  , p_auth_scheme              => :auth_scheme
  , p_auth_domain              => :auth_domain
  , po_create_status           => :status_code_out
  , po_create_message          => :status_message_out
  );
END;
