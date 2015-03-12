SELECT 1 precedence, fep.engine_config config_xml, 'FOX_ENVIRONMENT_PROPERTIES' "LOCATION"
FROM ${schema.fox}.fox_environment_properties fep
WHERE fep.environment_key = :environment_key
UNION ALL
SELECT 2 precedence, fe.engine_config config_xml, 'FOX_ENVIRONMENTS' "LOCATION"
FROM ${schema.fox}.fox_environments fe
WHERE fe.environment_key = :environment_key
ORDER BY precedence
