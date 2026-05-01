UPDATE `settings`
SET `value` = '¡Acabas de recibir tu regalo mensual del Club!'
WHERE `setting` = 'club.gift.present.label';

UPDATE `items` AS i
INNER JOIN `items_definitions` AS d ON d.`id` = i.`definition_id`
SET i.`custom_data` = REPLACE(
    i.`custom_data`,
    '|You have just received your monthly club gift!|',
    '|¡Acabas de recibir tu regalo mensual del Club!|'
)
WHERE d.`behaviour` LIKE '%present%'
  AND i.`custom_data` LIKE '%|You have just received your monthly club gift!|%';
