INSERT INTO ${schema.fox}.fox_upload_log( 
  file_id  
, app_mnem 
, app_host 
, wua_id 
, module 
, state 
, started_date 
, client_ip 
) 
VALUES ( 
  :file_id 
, :app_mnem 
, :app_host 
, :wua_id 
, :module 
, :state 
, sysdate 
, :client_ip 
)