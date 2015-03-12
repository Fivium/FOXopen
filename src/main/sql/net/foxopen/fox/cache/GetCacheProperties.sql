SELECT 
  fcc.cache_name
, fcc.cache_type
, fcc.environment_key
, fcc.engine_locator
, fcc.initial_capacity
, fcc.max_capacity
, fcc.use_weak_values
, fcc.concurrency_level
, fcc.time_to_live_ms
FROM ${schema.fox}.fox_cache_configuration fcc
WHERE fcc.engine_locator = :p_engine_locator
AND fcc.environment_key = :p_environment_key