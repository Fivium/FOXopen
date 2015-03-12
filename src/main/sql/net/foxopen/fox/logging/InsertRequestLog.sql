INSERT INTO ${schema.fox}.fox_request_log (
  id
, server_hostname
, server_context
, request_uri
, request_start_timestamp
, http_method
, query_string
, user_agent
, fox_session_id
, origin_ip
, forwarded_for
, log_created_timestamp
)
VALUES (
  :id
, :server_hostname
, :server_context
, :request_uri
, :request_start_timestamp
, :http_method
, :query_string
, :user_agent
, :fox_session_id
, :origin_ip
, :forwarded_for
, SYSTIMESTAMP
)
