SET NAMES utf8mb4;

UPDATE tb_user SET nick_name = 'Alex Fisher' WHERE id = 1;
UPDATE tb_user SET nick_name = 'Emma Lee' WHERE id = 2;
UPDATE tb_user SET nick_name = 'Olivia Chen' WHERE id = 5;

UPDATE tb_shop SET
  name = '103 Tea House',
  area = 'Daguan District',
  address = '29 Jinhua Road, Hangzhou',
  open_hours = '10:00-22:00'
WHERE id = 1;

UPDATE tb_voucher SET
  title = '$50 Regular Voucher',
  sub_title = 'Valid Monday to Sunday',
  rules = 'General use. Dine-in only. Non-refundable.'
WHERE id = 1;

UPDATE tb_voucher SET
  title = '$100 Flash Deal',
  sub_title = 'Limited time seckill',
  rules = 'One order per user. While stocks last.'
WHERE id = 10;

UPDATE tb_seckill_voucher SET stock = 487 WHERE voucher_id = 10;
UPDATE tb_seckill_voucher SET stock = 763 WHERE voucher_id = 11;
UPDATE tb_seckill_voucher SET stock = 318 WHERE voucher_id = 12;
UPDATE tb_seckill_voucher SET stock = 1042 WHERE voucher_id = 13;
UPDATE tb_seckill_voucher SET stock = 556 WHERE voucher_id = 14;

INSERT INTO tb_voucher (shop_id, title, sub_title, rules, pay_value, actual_value, type, status)
SELECT 1, '$30 Lunch Pass', 'Weekday lunch special', 'Valid 11:00-14:00 on weekdays', 2500, 3000, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM tb_voucher WHERE title = '$30 Lunch Pass');

INSERT INTO tb_voucher (shop_id, title, sub_title, rules, pay_value, actual_value, type, status)
SELECT 1, '$200 Premium Bundle', 'Weekend premium set', 'Reservation required', 15000, 20000, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM tb_voucher WHERE title = '$200 Premium Bundle');

INSERT INTO tb_voucher (shop_id, title, sub_title, rules, pay_value, actual_value, type, status)
SELECT 1, '$20 Coffee Combo', 'Morning rush deal', 'Valid before 11:00 AM', 1200, 2000, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM tb_voucher WHERE title = '$20 Coffee Combo');

INSERT INTO tb_voucher (shop_id, title, sub_title, rules, pay_value, actual_value, type, status)
SELECT 1, '$80 Dinner For Two', 'Share with a friend', 'Two-person dinner set', 6500, 8000, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM tb_voucher WHERE title = '$80 Dinner For Two');

INSERT INTO tb_seckill_voucher (voucher_id, stock, begin_time, end_time)
SELECT v.id, 763, '2026-01-01 00:00:00', '2026-12-31 23:59:59'
FROM tb_voucher v
LEFT JOIN tb_seckill_voucher sv ON v.id = sv.voucher_id
WHERE v.type = 1 AND v.title = '$30 Lunch Pass' AND sv.voucher_id IS NULL;

INSERT INTO tb_seckill_voucher (voucher_id, stock, begin_time, end_time)
SELECT v.id, 318, '2026-01-01 00:00:00', '2026-12-31 23:59:59'
FROM tb_voucher v
LEFT JOIN tb_seckill_voucher sv ON v.id = sv.voucher_id
WHERE v.type = 1 AND v.title = '$200 Premium Bundle' AND sv.voucher_id IS NULL;

INSERT INTO tb_seckill_voucher (voucher_id, stock, begin_time, end_time)
SELECT v.id, 1042, '2026-01-01 00:00:00', '2026-12-31 23:59:59'
FROM tb_voucher v
LEFT JOIN tb_seckill_voucher sv ON v.id = sv.voucher_id
WHERE v.type = 1 AND v.title = '$20 Coffee Combo' AND sv.voucher_id IS NULL;

INSERT INTO tb_seckill_voucher (voucher_id, stock, begin_time, end_time)
SELECT v.id, 556, '2026-01-01 00:00:00', '2026-12-31 23:59:59'
FROM tb_voucher v
LEFT JOIN tb_seckill_voucher sv ON v.id = sv.voucher_id
WHERE v.type = 1 AND v.title = '$80 Dinner For Two' AND sv.voucher_id IS NULL;
