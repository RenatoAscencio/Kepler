INSERT INTO external_texts (entry, text) VALUES
('cmd.setconfig.not_found', 'The setting "%setting%" doesn''t exist!'),
('cmd.setconfig.updated', 'The setting "%setting%" value has been updated from "%old%" to "%new%"'),
('cmd.setprice.not_found', 'That sale code doesn''t exist!'),
('cmd.setprice.not_number', 'You did not enter a number!'),
('cmd.setprice.same_price', 'You entered the same price that the catalogue item costs!'),
('cmd.setprice.updated', 'The %item% has successfully %word% from %old% to %new%')
ON DUPLICATE KEY UPDATE text = VALUES(text);
