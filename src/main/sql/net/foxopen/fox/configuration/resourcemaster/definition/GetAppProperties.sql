  SELECT 1 precedence, fap.app_config config_xml, 'FOX_APPLICATION_PROPERTIES' "LOCATION"
  FROM ${schema.fox}.fox_application_properties fap
  WHERE fap.environment_key = :environment_key
  AND fap.app_mnem = :app_mnem
UNION ALL
  SELECT 2 precedence, fa.app_config, 'FOX_APPLICATIONS' "LOCATION"
  FROM ${schema.fox}.fox_applications fa
  WHERE fa.environment_key = :environment_key
  AND fa.app_mnem = :app_mnem
UNION ALL
  SELECT 3 precedence, app_config , 'FOX_ENVIRONMENT_PROPERTIES' "LOCATION"
  FROM ${schema.fox}.fox_environment_properties fep
  WHERE  fep.environment_key = :environment_key
UNION ALL
  SELECT 4 precedence, fe.app_config, 'FOX_ENVIRONMENTS' "LOCATION"
  FROM ${schema.fox}.fox_environments fe
  WHERE fe.environment_key = :environment_key
ORDER BY precedence
