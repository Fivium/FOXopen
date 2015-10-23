BEGIN
  :render_xml := spatialmgr.spm_fox.generate_render_xml(
    p_canvas_usage_id => :canvas_usage_id
  , p_call_id         => :call_id
  , p_wua_id          => :wua_id
  , p_width_px        => :image_width_px
  , p_height_px       => :image_height_px
  , p_datasource      => :datasource
  );
END;
