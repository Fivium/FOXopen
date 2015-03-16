BEGIN

  ${schema.fox}.fox_engine.register_engine (
    p_engine_locator => :p_engine_locator
  , p_plugin_list => :p_plugin_list
  , p_environment_key => :p_environment_key
  , p_security_token => :p_security_token
  );

END;