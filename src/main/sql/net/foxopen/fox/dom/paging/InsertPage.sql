INSERT INTO ${schema.fox}.fox_pagination_pages (
  module_call_id
, match_id 
, invoke_name 
, row_number
, created_datetime 
, page_xml
)
VALUES (
  :call_id
, :match_id
, :invoke_name
, :row_num
, SYSDATE
, :page_xml
)