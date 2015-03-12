DECLARE
  l_blob BLOB;
BEGIN

  UPDATE ${schema.fox}.fox_module_call_facets
  SET last_updated_datetime = SYSDATE
  WHERE facet_type = :facet_type
  AND facet_key = :facet_key
  AND module_call_id = :module_call_id
  RETURNING facet_object INTO l_blob;

  :facet_object_blob := l_blob;

END;

