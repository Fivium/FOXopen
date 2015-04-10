SELECT image_blob, image_type, width, height
FROM foxmgr.fox_processed_images
WHERE file_id = :file_id
AND rotation = :rotation
AND process_type != 'ORIGINAL'
ORDER BY width + height