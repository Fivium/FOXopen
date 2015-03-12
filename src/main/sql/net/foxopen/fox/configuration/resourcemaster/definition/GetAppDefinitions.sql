SELECT fa.app_mnem 
FROM ${schema.fox}.fox_applications fa 
WHERE fa.environment_key = :environment_key