-- A user1 admin jogot kap (a rendelés-állapotok kezeléséhez).
-- Meglévő adatbázison ez frissíti a szerepkört; friss adatbázison a
-- UserSeeder már eleve ADMIN-ként hozza létre a user1-et.
UPDATE users SET role = 'ADMIN' WHERE username = 'user1' AND role <> 'ADMIN';
