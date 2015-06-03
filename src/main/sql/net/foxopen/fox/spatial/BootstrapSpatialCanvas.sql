BEGIN
  spatialmgr.spm.operation_start (
    p_calling_module => 'FOX SpatialEngine'
  , p_description    => 'bootstrapSpatialCanvas'
  , p_wua_id         => :wua_id
  );
  :bootstrap_xml := spatialmgr.spm_fox.get_canvas(
    p_xml_data    => :xml_data
  , p_call_id     => :call_id
  , p_wua_id      => :wua_id
  );
  spatialmgr.spm.operation_end();
END;
