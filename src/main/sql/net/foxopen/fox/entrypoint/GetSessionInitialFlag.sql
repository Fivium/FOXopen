BEGIN
  SELECT
    fs.initial_flag
  INTO :flag_value
  FROM ${schema.fox}.fox_sessions fs
  WHERE fs.id = :fox_session_id;

  UPDATE ${schema.fox}.fox_sessions fs
  SET fs.initial_flag = null
  WHERE fs.id = :fox_session_id;
END;