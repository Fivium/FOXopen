SELECT fe.pool_name
FROM ${schema.fox}.fox_connections fe
WHERE fe.environment_key = :p_environment_key
AND fe.engine_locator = :p_engine_locator
AND fe.default_connection = 'true'