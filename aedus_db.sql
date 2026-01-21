CREATE DATABASE IF NOT EXISTS aedusdb;
USE aedusdb;

-- 1. Tabla de Roles
CREATE TABLE IF NOT EXISTS roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE
);

-- 2. Tabla de Usuarios
CREATE TABLE IF NOT EXISTS usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    rol_id INT,
    FOREIGN KEY (rol_id) REFERENCES roles(id) ON DELETE SET NULL
);

-- 3. Tabla de Aulas (Ubicaciones)
CREATE TABLE IF NOT EXISTS aulas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    capacidad INT
);

-- 4. Tabla de Categorías de Incidencias
CREATE TABLE IF NOT EXISTS categorias (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE
);

-- 5. Tabla de Estados de Incidencias
CREATE TABLE IF NOT EXISTS estados (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE
);

-- 6. Tabla de Incidencias
CREATE TABLE IF NOT EXISTS incidencias (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    descripcion TEXT,
    usuario_id INT,
    aula_id INT,
    categoria_id INT,
    estado_id INT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL,
    FOREIGN KEY (aula_id) REFERENCES aulas(id) ON DELETE SET NULL,
    FOREIGN KEY (categoria_id) REFERENCES categorias(id) ON DELETE SET NULL,
    FOREIGN KEY (estado_id) REFERENCES estados(id) ON DELETE SET NULL
);

-- 7. Tabla de Recursos
CREATE TABLE IF NOT EXISTS recursos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    cantidad INT NOT NULL,
    aula_id INT,
    FOREIGN KEY (aula_id) REFERENCES aulas(id) ON DELETE SET NULL
);

-- INSERTAR DATOS DE PRUEBA

-- Roles
INSERT INTO roles (nombre) VALUES ('Administrador'), ('Profesor'), ('Mantenimiento');

-- Categorias
INSERT INTO categorias (nombre) VALUES ('Hardware'), ('Software'), ('Conectividad'), ('Mobiliario');

-- Estados
INSERT INTO estados (nombre) VALUES ('Pendiente'), ('En Progreso'), ('Resuelto'), ('Cerrado');




