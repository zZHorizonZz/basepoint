-- The tables and rows that every contract test starts from.
-- {json}, {timestamp}, {text}, {bool} and {double} are the type of the database for such a column, and [[name]] is the
-- name quoted by the dialect. A statement ends with a semicolon at the end of a line.

CREATE TABLE orders (id INT PRIMARY KEY, state VARCHAR(20) NOT NULL, total INT NOT NULL, region VARCHAR(10),
  note VARCHAR(50), paid {bool});
INSERT INTO orders (id, state, total, region, note, paid) VALUES
  (1, 'OPEN', 150, 'EU', NULL, '1'),
  (2, 'OPEN', 80, 'US', 'rush', '0'),
  (3, 'CLOSED', 300, 'EU', 'gift', '1'),
  (4, 'CANCELLED', 50, NULL, NULL, NULL),
  (5, 'OPEN', 220, 'US', NULL, '0');

CREATE TABLE items (id INT PRIMARY KEY, order_id INT NOT NULL, sku VARCHAR(20) NOT NULL, qty INT NOT NULL);
INSERT INTO items (id, order_id, sku, qty) VALUES
  (10, 1, 'MUG', 2),
  (11, 1, 'CUP', 1),
  (12, 1, 'a%b_c', 3),
  (13, 2, 'MUG', 5),
  (14, 3, 'x[1]', 1),
  (15, 5, 'PLATE', 4);

CREATE TABLE products (sku VARCHAR(20) PRIMARY KEY, name VARCHAR(50) NOT NULL, price DECIMAL(10, 2) NOT NULL);
INSERT INTO products (sku, name, price) VALUES ('MUG', 'Mug', 4.50), ('CUP', 'Cup', 3.25), ('PLATE', 'Plate', 12.00);

CREATE TABLE archived (id INT PRIMARY KEY, state VARCHAR(20) NOT NULL);
INSERT INTO archived (id, state) VALUES (1, 'OPEN'), (6, 'CLOSED'), (7, 'OPEN');

CREATE TABLE defaults (id INT NOT NULL DEFAULT 7, label VARCHAR(10) NOT NULL DEFAULT 'x');

CREATE TABLE categories (id INT PRIMARY KEY, parent_id INT, name VARCHAR(20) NOT NULL);
INSERT INTO categories (id, parent_id, name) VALUES
  (1, NULL, 'Home'),
  (2, 1, 'Kitchen'),
  (3, 1, 'Garden'),
  (4, 2, 'Mugs'),
  (5, 4, 'Tea mugs'),
  (6, NULL, 'Office'),
  (7, 6, 'Desks');

CREATE TABLE labelled (id INT PRIMARY KEY, labels {json});
INSERT INTO labelled (id, labels) VALUES
  (1, '{"env": "prod", "team": "core"}'),
  (2, '{"env": "dev", "team": "web"}'),
  (3, '{"env": "prod", "app.kubernetes.io/name": "shop"}'),
  (4, '{}'),
  (5, NULL),
  (6, '{"replicas": 3}');

CREATE TABLE events (id INT PRIMARY KEY, happened {timestamp} NOT NULL);

CREATE TABLE texts (id INT PRIMARY KEY, body {text});

CREATE TABLE numbers (id INT PRIMARY KEY, whole BIGINT, exact DECIMAL(38, 10), approximate {double});

CREATE TABLE [[odd "names"]] (id INT PRIMARY KEY, [[select]] VARCHAR(20), [[with space]] VARCHAR(20),
  [[MixedCase]] VARCHAR(20), [[a"b]] VARCHAR(20), [[a`b]] VARCHAR(20), [[a]b]] VARCHAR(20), [[it's]] VARCHAR(20),
  [[ünï]] VARCHAR(20));
INSERT INTO [[odd "names"]] (id, [[select]], [[with space]], [[MixedCase]], [[a"b]], [[a`b]], [[a]b]], [[it's]], [[ünï]])
  VALUES (1, 's1', 'w1', 'm1', 'q1', 'b1', 'r1', 'i1', 'u1'), (2, 's2', 'w2', 'm2', 'q2', 'b2', 'r2', 'i2', 'u2');
