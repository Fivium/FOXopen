SELECT *
FROM ${schema.fox}.fox_engine_plugins fep
WHERE fep.environment_key = :p_environment_key
AND fep.engine_locator = :p_engine_locator