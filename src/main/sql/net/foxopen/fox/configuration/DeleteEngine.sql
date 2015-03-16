BEGIN

  ${schema.fox}.fox_engine.delete_engine (
    p_environment_key => :p_environment_key
  , p_engine_locator => :p_engine_locator
  );

END;