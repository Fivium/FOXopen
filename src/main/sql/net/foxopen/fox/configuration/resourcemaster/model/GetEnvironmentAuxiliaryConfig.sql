SELECT clob_data, blob_data
FROM ${schema.fox}.fox_environment_aux_config
WHERE environment_key = :environment_key
AND config_mnem = :config_mnem
