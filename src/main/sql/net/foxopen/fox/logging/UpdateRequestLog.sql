UPDATE ${schema.fox}.fox_request_log
SET 
  request_end_timestamp = :request_end_timestamp
, response_code = :response_code
, log_updated_timestamp = SYSTIMESTAMP
WHERE id = :id