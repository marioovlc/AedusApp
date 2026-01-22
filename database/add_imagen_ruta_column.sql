-- Script de migración para agregar soporte de imágenes a incidencias
-- Ejecutar este script en la base de datos Neon PostgreSQL

-- Agregar columna para almacenar la ruta de la imagen adjunta
ALTER TABLE incidencias 
ADD COLUMN IF NOT EXISTS imagen_ruta VARCHAR(500);

-- Comentario para documentar la columna
COMMENT ON COLUMN incidencias.imagen_ruta IS 'Ruta relativa de la imagen adjunta a la incidencia (puede ser NULL)';
