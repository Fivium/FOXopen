BEGIN
  :operation_xml := spatialmgr.spm_fox.process_canvas_event(
    p_event_label => :event_label
  , p_xml_data    => :xml_data
  );
END;
