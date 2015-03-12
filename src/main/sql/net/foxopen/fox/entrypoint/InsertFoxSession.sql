INSERT INTO ${schema.fox}.fox_sessions (id, new_id, wus_id, initial_flag, created_datetime)
VALUES (:fox_session_id, null, :current_wus_id, :initial_flag, sysdate) 