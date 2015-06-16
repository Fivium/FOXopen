BEGIN
  INSERT INTO ${schema.fox}.fox_module_calls (
    call_id
  , stack_position
  , thread_id
  , app_mnem
  , module_name
  , theme_name
  , storage_locations
  , callback_handlers
  , security_scope
  , xpath_variables
  , created_datetime
  , last_updated_datetime
  )
  VALUES (
    :call_id
  , :stack_position
  , :thread_id
  , :app_mnem
  , :module_name
  , :theme_name
  , :storage_locations
  , :callback_handlers
  , :security_scope
  , EMPTY_BLOB()
  , SYSDATE
  , SYSDATE
  )
  RETURNING xpath_variables INTO :xpath_variables;
END;