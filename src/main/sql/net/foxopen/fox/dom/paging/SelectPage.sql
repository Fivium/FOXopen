SELECT page_xml 
FROM ${schema.fox}.fox_pagination_pages
WHERE module_call_id = :call_id
AND match_id = :match_id
AND invoke_name = :invoke_name
AND row_number BETWEEN :row_start AND :row_end
ORDER BY row_number 