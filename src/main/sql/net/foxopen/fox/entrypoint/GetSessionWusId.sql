SELECT
  fs.wus_id
FROM ${schema.fox}.fox_sessions fs
WHERE fs.id = :fox_session_id
