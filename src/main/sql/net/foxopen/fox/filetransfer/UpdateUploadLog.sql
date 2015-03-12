UPDATE ${schema.fox}.fox_upload_log SET 
  file_approx_size = :file_approx_size 
, file_content_type = :file_content_type 
, completed_date = CASE WHEN :status = 'SUCCESS' THEN sysdate ELSE null END  
, failed_date = CASE WHEN :status = 'FAIL' THEN sysdate ELSE null END 
, fail_reason = CASE WHEN :status = 'FAIL' THEN :fail_reason ELSE null END 
, magic_mime_types = :magic_mime_types 
, filename = :filename 
, xml_data = :xml_data 
WHERE file_id = :file_id