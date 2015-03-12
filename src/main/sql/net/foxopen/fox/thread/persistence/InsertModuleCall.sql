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
, SYSDATE
, SYSDATE
)