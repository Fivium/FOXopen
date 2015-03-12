UPDATE ${schema.fox}.fox_request_log
SET
  user_experience_time_ms = :user_experience_time_ms
, user_experience_detail_xml = :user_experience_detail_xml
WHERE id = :id
AND user_experience_time_ms IS NULL