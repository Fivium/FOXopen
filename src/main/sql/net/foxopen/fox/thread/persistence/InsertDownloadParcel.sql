INSERT INTO ${schema.fox}.fox_download_parcels ( 
  id 
, thread_id 
, data_clob 
, created_datetime 
) 
VALUES ( 
  :parcel_id 
, :thread_id 
, :data_clob 
, SYSDATE   
)