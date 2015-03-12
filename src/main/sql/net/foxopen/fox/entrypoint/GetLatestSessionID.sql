SELECT
  fs.id
FROM ${schema.fox}.fox_sessions fs
WHERE fs.new_id is null
START WITH fs.id = :1
CONNECT BY fs.id = PRIOR fs.new_id