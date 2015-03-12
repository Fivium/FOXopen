SELECT config_xml
FROM ${schema.fox}.fox_plugin_config fpc
WHERE fpc.plugin_name = :p_plugin_name