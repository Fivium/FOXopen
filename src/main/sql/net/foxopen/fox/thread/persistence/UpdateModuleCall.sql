UPDATE ${schema.fox}.fox_module_calls 
SET 
  security_scope = :security_scope 
, last_updated_datetime = SYSDATE 
WHERE call_id = :call_id  