BEGIN
  :session_id_out := ${schema.auth}.authentication.session_create(
    p_origin_client_info       => :client_info 
  , p_application_display_name => :app_display_name 
  , p_login_id                 => :login_id
  , p_password                 => :password
  , p_service_list             => :service_list
  , po_create_status           => :status_code_out
  , po_create_message          => :status_message_out
  );
END; 