BEGIN
  INSERT INTO ${schema.fox}.fox_threads (
    thread_id
  , app_mnem
  , user_thread_session_id
  , change_number
  , thread_property_map
  , field_set
  , authentication_context
  , fox_session_id
  , created_datetime
  , last_updated_datetime
  )
  VALUES (
    :thread_id
  , :app_mnem
  , :user_thread_session_id
  , :change_number
  , EMPTY_BLOB()
  , EMPTY_BLOB()
  , :authentication_context
  , :fox_session_id
  , SYSDATE
  , SYSDATE
  )
  RETURNING thread_property_map, field_set INTO :property_map_blob, :field_set_blob;

END;
