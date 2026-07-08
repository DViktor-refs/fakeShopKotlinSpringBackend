-- E-mail mező a felhasználókhoz (az Android app /api/auth/me válaszához kell).
ALTER TABLE users ADD COLUMN email VARCHAR(255);

-- A már létező (korábban seedelt) felhasználóknak visszamenőleg is adunk e-mailt,
-- ugyanazzal a mintával, amit a UserSeeder is használ friss telepítésnél.
UPDATE users SET email = username || '@fakeshop.test' WHERE email IS NULL;

ALTER TABLE users ALTER COLUMN email SET NOT NULL;
