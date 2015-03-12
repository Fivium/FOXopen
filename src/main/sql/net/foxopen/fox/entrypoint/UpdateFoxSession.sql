UPDATE ${schema.fox}.fox_sessions
SET new_id = :new_session_id
, initial_flag = null
WHERE id = (
  SELECT
    fs.id old_tip_id
  FROM ${schema.fox}.fox_sessions fs
  WHERE fs.new_id is null
  START WITH fs.id = :old_session_id
  CONNECT BY fs.id = PRIOR fs.new_id
)