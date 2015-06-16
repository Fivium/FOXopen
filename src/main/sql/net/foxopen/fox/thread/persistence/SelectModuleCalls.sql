SELECT
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
FROM ${schema.fox}.fox_module_calls
WHERE thread_id = :thread_id
ORDER BY stack_position