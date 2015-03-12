UPDATE ${schema.fox}.fox_state_calls 
SET  
  scroll_position = :scroll_position 
, context_labels = :context_labels 
, last_updated_datetime = SYSDATE
WHERE call_id = :call_id