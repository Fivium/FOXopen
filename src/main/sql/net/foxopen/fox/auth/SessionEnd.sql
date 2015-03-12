BEGIN
  ${schema.auth}.authentication.session_end(:client_info, :app_display_name, :wua_id, :wua_id, :session_id, :status_code, :status_message);
END;