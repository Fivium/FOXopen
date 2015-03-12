BEGIN

  UPDATE ${schema.fox}.fox_threads
  SET
    authentication_context = :authentication_context
  , change_number = :change_number
  , fox_session_id = :fox_session_id
  , last_updated_datetime = SYSDATE
  WHERE thread_id = :thread_id
  RETURNING thread_property_map, field_set INTO :property_map_blob, :field_set_blob;

END;
