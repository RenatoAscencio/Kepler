UPDATE `catalogue_items`
SET `definition_id` = (
    SELECT `id` FROM `items_definitions`
    WHERE `sprite` = 'ads_idol_clRack'
    LIMIT 1
)
WHERE `definition_id` = (
    SELECT `id` FROM `items_definitions`
    WHERE `sprite` = 'clrack'
    LIMIT 1
);

UPDATE `items`
SET `definition_id` = (
    SELECT `id` FROM `items_definitions`
    WHERE `sprite` = 'ads_idol_clRack'
    LIMIT 1
)
WHERE `definition_id` = (
    SELECT `id` FROM `items_definitions`
    WHERE `sprite` = 'clrack'
    LIMIT 1
);
