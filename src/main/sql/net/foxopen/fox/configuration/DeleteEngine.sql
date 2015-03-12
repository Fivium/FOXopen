DECLARE
  l_status VARCHAR2(4000); 
BEGIN 
   
  l_status := ${schema.fox}.fox_engine.delete_engine ( 
    p_environment_key => :p_environment_key
  , p_engine_locator => :p_engine_locator 
  );
  
  :status_out := l_status; 

END;