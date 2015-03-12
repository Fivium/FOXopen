SELECT 
  d.module_call_id 
, d.document_name 
, d.xml_data xml_data 
FROM ${schema.fox}.fox_internal_doms d 
WHERE module_call_id = :call_id 
AND document_name = :doc_name