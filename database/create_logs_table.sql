-- Script para crear la tabla de logs
-- Ejecutar este script en la base de datos Neon PostgreSQL

CREATE TABLE IF NOT EXISTS logs (
    id SERIAL PRIMARY KEY,
    usuario_id INTEGER REFERENCES usuarios(id) ON DELETE SET NULL,
    accion VARCHAR(50) NOT NULL,
    categoria VARCHAR(30) NOT NULL,
    descripcion TEXT NOT NULL,
    ip_address VARCHAR(45),
    fecha_creacion TIMESTAMP DEFAULT NOW()
);

-- Índices para mejorar el rendimiento de consultas
CREATE INDEX IF NOT EXISTS idx_logs_categoria ON logs(categoria);
CREATE INDEX IF NOT EXISTS idx_logs_usuario_id ON logs(usuario_id);
CREATE INDEX IF NOT EXISTS idx_logs_fecha ON logs(fecha_creacion DESC);

-- Comentarios para documentar
COMMENT ON TABLE logs IS 'Tabla para registrar todas las acciones importantes del sistema';
COMMENT ON COLUMN logs.accion IS 'Tipo de acción: LOGIN, LOGOUT, CREAR, ACTUALIZAR, ELIMINAR';
COMMENT ON COLUMN logs.categoria IS 'Categoría: LOGIN, INCIDENCIA, USUARIO, SISTEMA, ERROR';
