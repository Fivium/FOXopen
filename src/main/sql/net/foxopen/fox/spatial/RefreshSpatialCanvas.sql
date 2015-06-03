BEGIN
  :refresh_xml := spatialmgr.spm_fox.refresh_canvas(
    p_xml_data    => :operation_xml
  );
END;
