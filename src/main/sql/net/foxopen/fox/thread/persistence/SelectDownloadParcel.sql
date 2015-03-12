SELECT 
  d.data_clob 
FROM ${schema.fox}.fox_download_parcels d 
WHERE d.id = :parcel_id 
AND d.thread_id = :thread_id