INSERT INTO ${schema.fox}.fox_state_calls ( 
  call_id 
, stack_position 
, module_call_id 
, state_name 
, scroll_position 
, context_labels 
, created_datetime 
, last_updated_datetime
) 
VALUES ( 
  :call_id 
, :stack_position 
, :module_call_id 
, :state_name 
, :scroll_position 
, :context_labels 
, SYSDATE
, SYSDATE
)