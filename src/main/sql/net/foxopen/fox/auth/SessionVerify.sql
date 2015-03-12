DECLARE
  r VARCHAR2(100);
  l_user_dom_current BOOLEAN;
BEGIN
  r  := ${schema.auth}.authentication.session_verify(
          p_origin_client_info => :client_info
        , p_application_display_name => :app_display_name
        , p_session_id => :session_id
        , po_wua_id => :wua_id_out
        , po_verify_status => :status_out
        , po_verify_message => :message_out
        , po_timeout_mins => :timeout_mins_out
        , p_user_dom_change_number => :user_dom_change_no
        , po_user_dom_current => l_user_dom_current
        );
  :user_dom_current := CASE l_user_dom_current WHEN TRUE THEN 'true' ELSE 'false' END;
  :session_id_out := NVL(r,'NULL');
END;
