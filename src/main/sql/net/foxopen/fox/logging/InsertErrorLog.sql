INSERT INTO ${schema.fox}.fox_errors (
  error_ref
, error_type
, error_detail
, server_hostname
, server_context
, request_id
, error_occurred_timestamp
, log_written_timestamp
)
VALUES (
  :error_ref
, :error_type
, :error_detail
, :server_hostname
, :server_context
, :request_id
, :error_occurred_timestamp
, SYSTIMESTAMP
)
