CREATE type brick_color as enum('red', 'blue', 'green', 'yellow', 'black', 'gray', 'pink');

CREATE TABLE bricks (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  price BIGINT NOT NULL,
  color brick_color NOT NULL
);
