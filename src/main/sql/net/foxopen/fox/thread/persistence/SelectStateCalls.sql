SELECT 
  call_id 
, stack_position 
, module_call_id 
, state_name 
, scroll_position 
, context_labels 
FROM ${schema.fox}.fox_state_calls 
WHERE module_call_id = :call_id 
ORDER BY stack_position 