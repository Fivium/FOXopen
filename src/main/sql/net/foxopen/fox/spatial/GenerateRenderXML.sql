BEGIN
  :render_xml := spatialmgr.spm_fox.generate_render_xml(
    p_sc_id      => :spatial_canvas_id
  , p_width_px   => :image_width_px
  , p_height_px  => :image_height_px
  , p_datasource => :datasource
  );
END;
