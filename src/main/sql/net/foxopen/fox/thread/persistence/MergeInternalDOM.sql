MERGE INTO ${schema.fox}.fox_internal_doms d 
USING ( 
  SELECT 
    :module_call_id module_call_id 
  , :document_name document_name 
  , :xml_data xml_data 
  FROM dual  
) q  
ON (d.module_call_id = q.module_call_id AND d.document_name = q.document_name) 
WHEN NOT MATCHED THEN INSERT (module_call_id, document_name, xml_data) 
VALUES (q.module_call_id, q.document_name, q.xml_data) 
WHEN MATCHED THEN UPDATE SET xml_data = q.xml_data