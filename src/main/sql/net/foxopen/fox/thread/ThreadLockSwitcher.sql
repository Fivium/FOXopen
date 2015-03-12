UPDATE ${schema.fox}.fox_threads  
SET locked_by = :1, locked_since_datetime = :2  
WHERE thread_id = :3