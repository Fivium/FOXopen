DELETE FROM ${schema.fox}.fox_module_call_facets
WHERE facet_type = :facet_type
AND facet_key = :facet_key
AND module_call_id = :module_call_id