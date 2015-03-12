SELECT
  thread_id
, app_mnem
, user_thread_session_id
, change_number
, thread_property_map
, field_set
, authentication_context
, fox_session_id
FROM ${schema.fox}.fox_threads
WHERE thread_id = :thread_id
