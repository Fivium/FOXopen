DECLARE
  l_blob BLOB;
BEGIN

  INSERT INTO ${schema.fox}.fox_module_call_facets (
    facet_type
  , facet_key
  , module_call_id
  , facet_object
  , created_datetime
  )
  VALUES (
    :facet_type
  , :facet_key
  , :module_call_id
  , empty_blob()
  , SYSDATE
  )
  RETURNING facet_object INTO l_blob;

  :facet_object_blob := l_blob;

END;
