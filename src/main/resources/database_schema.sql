-- Schema Creation
CREATE SCHEMA IF NOT EXISTS gestion_incidencias;
SET search_path TO gestion_incidencias;

-- 1. Roles
CREATE TABLE IF NOT EXISTS roles (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) UNIQUE NOT NULL
);

-- 2. Usuarios
CREATE TABLE IF NOT EXISTS usuarios (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    rol_id INT DEFAULT 0 -- Field kept for legacy compatibility
);

-- 3. Usuario_Roles (Many-to-Many)
CREATE TABLE IF NOT EXISTS usuario_roles (
    usuario_id INT REFERENCES usuarios(id) ON DELETE CASCADE,
    rol_id INT REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (usuario_id, rol_id)
);

-- 4. Estados
CREATE TABLE IF NOT EXISTS estados (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) UNIQUE NOT NULL
);

-- 5. Categorias
CREATE TABLE IF NOT EXISTS categorias (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) UNIQUE NOT NULL
);

-- 6. Aulas
CREATE TABLE IF NOT EXISTS aulas (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) UNIQUE NOT NULL,
    capacidad INT DEFAULT 30
);

-- 7. Incidencias
CREATE TABLE IF NOT EXISTS incidencias (
    id SERIAL PRIMARY KEY,
    titulo VARCHAR(200) NOT NULL,
    descripcion TEXT,
    usuario_id INT REFERENCES usuarios(id) ON DELETE SET NULL,
    aula_id INT REFERENCES aulas(id) ON DELETE SET NULL,
    categoria_id INT REFERENCES categorias(id) ON DELETE SET NULL,
    aula_tipo VARCHAR(50),
    estado_id INT REFERENCES estados(id) ON DELETE SET NULL,
    fecha_creacion TIMESTAMP DEFAULT NOW(),
    imagen_ruta VARCHAR(255),
    resolucion TEXT
);

-- 8. Logs
CREATE TABLE IF NOT EXISTS logs (
    id SERIAL PRIMARY KEY,
    usuario_id INT REFERENCES usuarios(id) ON DELETE SET NULL,
    accion VARCHAR(100),
    categoria VARCHAR(50),
    descripcion TEXT,
    ip_address VARCHAR(50),
    fecha_creacion TIMESTAMP DEFAULT NOW()
);

-- ==========================================
-- INITIAL DATA POPULATION
-- ==========================================

-- Roles
INSERT INTO roles (id, nombre) VALUES 
(0, 'Sin Rol'),
(1, 'Administrador'),
(2, 'Profesor'),
(3, 'Mantenimiento')
ON CONFLICT (id) DO NOTHING;

-- Estados (Essential for IncidenciaDAO)
INSERT INTO estados (nombre) VALUES 
('NO LEIDO'), 
('LEIDO'), 
('EN REVISION'), 
('ACABADO') 
ON CONFLICT (nombre) DO NOTHING;

-- Categorias (Example data)
INSERT INTO categorias (nombre) VALUES 
('Hardware'), ('Software'), ('Red'), ('Mobiliario'), ('Eléctrico'), ('Otros')
ON CONFLICT (nombre) DO NOTHING;

-- Aulas (Example data)
INSERT INTO aulas (nombre, capacidad) VALUES 
('Aula 1.1', 30), ('Aula 1.2', 30), ('Informática 1', 25), ('Informática 2', 25), ('Laboratorio', 20), ('Biblioteca', 100)
ON CONFLICT (nombre) DO NOTHING;

-- Admin User (Ensure at least one user exists)
INSERT INTO usuarios (nombre, email, password, status, rol_id) 
VALUES ('Admin', 'admin@aedus.com', 'admin123', 'ACTIVE', 1) 
ON CONFLICT (email) DO NOTHING;

-- 8. Dummy Incidents (REMOVED)
-- INSERT INTO incidencias ... (Eliminado para que empiece limpio)
