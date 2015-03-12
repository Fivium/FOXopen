DECLARE
  l_privs VARCHAR2(32767);
  l_change_no VARCHAR2(32767);
BEGIN
  :user_dom := ${schema.auth}.authentication.get_user_dom(
    p_session_id => :session_id
  , p_current_change_number => :current_change_number
  , p_csv_system_privs => :csv_sys_privs
  , p_csv_urefs => :csv_uref_list
  , p_csv_uref_privs => :csv_uref_privs
  , p_csv_uref_types => :csv_uref_types
  , po_csv_privs => l_privs
  , po_latest_change_number => l_change_no
  );
  :privs_csv := l_privs;
  :latest_change_number := l_change_no;
END;
