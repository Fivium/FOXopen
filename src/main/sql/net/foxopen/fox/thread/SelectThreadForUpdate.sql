SELECT change_number, locked_by, locked_since_datetime  
FROM ${schema.fox}.fox_threads  
WHERE thread_id = :1 
FOR UPDATE NOWAIT