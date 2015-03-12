SELECT facet_object
FROM ${schema.fox}.fox_module_call_facets
WHERE facet_type = :facet_type
AND module_call_id = :module_call_id

