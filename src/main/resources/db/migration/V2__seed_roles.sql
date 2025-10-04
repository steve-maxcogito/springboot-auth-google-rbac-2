-- src/main/resources/db/migration/V2__seed_roles.sql
INSERT INTO role (id, name) VALUES
                                ('0d2a7d6c-6f4f-4d58-9a2b-6b9a1a9e0001','ROLE_USER'),
                                ('0d2a7d6c-6f4f-4d58-9a2b-6b9a1a9e0002','ROLE_ADMIN'),
                                ('0d2a7d6c-6f4f-4d58-9a2b-6b9a1a9e0003','ROLE_SECURITY_SERVICE'),
                                ('0d2a7d6c-6f4f-4d58-9a2b-6b9a1a9e0004','ROLE_DATA_SERVICE')
ON CONFLICT (name) DO NOTHING;
