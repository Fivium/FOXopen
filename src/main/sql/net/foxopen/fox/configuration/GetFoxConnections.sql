SELECT
  fc.pool_name
, fc.username
, fc.min_pool_size
, fc.max_pool_size
, fc.max_recycles
, fc.connection_timeout_ms
, fc.connection_init_sql
, fc.connection_checkin_sql
, fc.connection_checkout_sql
, fc.default_connection
FROM ${schema.fox}.fox_connections fc
WHERE fc.engine_locator = :p_engine_locator
AND fc.environment_key = :p_environment_key
