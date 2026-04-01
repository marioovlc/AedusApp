# Especificación Detallada: AedusApp (Migración a Flutter)

Este documento contiene la arquitectura, el diseño y las funcionalidades exactas de la aplicación **AedusApp** para su replicación 1:1 en Flutter.

---

## 1. Identidad Visual y Diseño (Premium Dark Theme)

La aplicación utiliza un diseño **Premium Dark** con un enfoque en la legibilidad, micro-animaciones y jerarquía visual clara.

### Paleta de Colores (Tokens)
- **Fondo Principal (Background):** `#060d1c` (Negro azulado profundo).
- **Superficie (Surface):** `#0d1629` (Para contenedores secundarios).
- **Tarjetas (Cards):** `#111f36` (Gris azulado oscuro).
- **Bordes:** `#1c2d47` (Sutil, para separación).
- **Acento Primario (Blue):** `#4f8ef7` (Azul vibrante para acciones principales).
- **Acento Secundario (Indigo):** `#818cf8` (Para elementos de estado o selección).
- **Texto Alta Prioridad:** `#f1f5f9` (Blanco azulado).
- **Texto Baja Prioridad:** `#64748b` (Gris tenue).
- **Éxitos (Success):** `#059669` (Verde esmeralda).
- **Peligro (Danger/Alert):** `#dc2626` (Rojo intenso).
- **Oro (AeduCoins):** `#fcd34d` (Amarillo brillante).

### Tipografía
- **Fuente Base:** `Segoe UI` (o `Inter` en Flutter).
- **Tamaño Base:** `14px`.
- **Títulos:**
  - `page-title`: `30px`, Bold, con sombra paralela (`dropshadow`).
  - `h1`: `24px`, Bold.
  - `h2`: `18px`, Bold.

---

## 2. Arquitectura de Navegación

La aplicación se basa en una estructura de **Sidebar Lateral** fija (izquierda) y un **Área de Contenido Dinámica** (centro).

### Sidebar (260px de ancho)
- **Logo:** Logo oficial de Aedus centrado en la parte superior.
- **Sección PRINCIPAL:**
  - `Dashboard`: Icono `tachometer-alt`. (Activo por defecto).
  - `Incidencias`: Icono `exclamation-triangle`. Incluye un **Badge numérico** rojo para notificaciones.
  - `Connect Hub`: Icono `comments`. (Centro de mensajería).
- **Sección ADMINISTRACIÓN:**
  - `Usuarios`: Icono `users`.
  - `Monitorización`: Icono `chart-line`.
  - `Logs de Sistema`: Icono `history`.
  - `Tienda`: Icono de tienda personalizado (AeduShop).
- **Tarjeta de Usuario (Inferior):**
  - Avatar circular con gradiente (`#4f8ef7` a `#818cf8`) y letras iniciales.
  - Nombre del usuario y Rol (Administrador, Profesor, Mantenimiento).
  - Contador de **AeduCoins** (🪙).
- **Acciones Finales:**
  - `Configuración`: Icono `cog`.
  - `Cerrar Sesión`: Icono `sign-out-alt` (Hover rojo).

---

## 3. Desglose por Pantallas

### A. Dashboard (Vista General)
- **KPI Cards (4):** Resumen numérico con iconos de colores.
  1. *Total Incidencias* (Azul).
  2. *Pendientes* (Naranja).
  3. *Resueltas* (Verde).
  4. *Usuarios Activos* (Púrpura).
- **Gráficos:**
  - Evolución de incidencias (Gráfico de líneas, últimos 7 días).
  - Incidencias por categoría (Gráfico de barras).
- **Gamificación:**
  - Lista de "Mis Logros Recientes" (Achievements).
  - Lista de "Mis Misiones Diarias" (Missions).
- **Asistente AI:** Un cuadro de texto donde la IA genera un resumen ejecutivo del estado del sistema en tiempo real.

### B. Gestión de Incidencias
- **Formulario de Creación (Izquierda):**
  - Título, Aula (selector + botón añadir nueva), Categoría, Descripción.
  - Botón de "Adjuntar Imagen 📎".
  - **Sugerencia IA (Botón ✨):** Al pulsarlo, carga una sugerencia dinámica de cómo el usuario podría resolver la incidencia por sí mismo basándose en el título/descripción.
- **Lista de Mis Tickets (Derecha):**
  - Scroll infinito con tarjetas de incidencia.
  - Cada tarjeta muestra: Título, Aula, Estado (Badge de color), Fecha, y una miniatura de la imagen si existe.

### C. Connect Hub (Centro de Comunicación)
- **Triple Panel:**
  1. **Lista de Contactos (Izquierda):**
     - Selector (Tab) entre "Tickets" (Chats de incidencias) y "Personas" (Compañeros).
     - Buscador integrado.
     - Ítem especial: `Aedus AI` (Soporte inteligente).
  2. **Área de Chat (Centro):**
     - Estilo "WhatsApp" con burbujas redondeadas.
     - Mi mensaje: Azul (`#2563eb`). Mensaje ajeno: Gris (`#1c2d47`).
     - Soporte para: Texto, Imágenes, y **Mensajes de Voz** (botón micrófono que detecta pulsación larga).
     - Barra de herramientas con adjuntar y grabar voz.
  3. **Detalles Contextuales (Derecha):**
     - Si el chat es de un Ticket: Muestra toda la info del ticket, imagen ampliada y botón de "Compartir en chat".
     - Si el chat es con una Persona: Muestra su Perfil, Bio, Email y Teléfono (editable si es el perfil propio).

### D. Monitorización & Admin
- **Usuarios:** Tabla avanzada con edición, asignación de roles y estados (Activo/Pendiente).
- **Monitorización:** Gráficos avanzados de rendimiento del sistema y carga de trabajo de mantenimiento.
- **Logs:** Histórico detallado de acciones realizadas por cada usuario con IP y Timestamp.

---

## 4. Lógica y Datos (Backend)

### Modelo de Datos (PostgreSQL)
- **Usuarios:** Nombre, Email, Password (encriptado), Rol, Status (PENDING, ACTIVE, BANNED), AeduCoins.
- **Incidencias:** Título, Descripción, UsuarioID, AulaID, CategoriaID, EstadoID (NO LEIDO, LEIDO, EN REVISION, ACABADO), ImagenRuta.
- **Roles:** Administrador, Profesor, Mantenimiento.
- **Aulas:** Nombre, Capacidad.
- **Logs:** Auditoría completa de acciones.

### Integraciones Especiales
- **IA (Gemini/OpenAI):** Utilizada para resumir el Dashboard y ofrecer soluciones sugeridas en la pantalla de tickets.
- **Sistema de Archivos:** Gestión de imágenes subidas para incidencias (ubicadas en carpeta `uploads` o Cloud Storage).
- **Gamificación:** Sistema de recompensas basado en la resolución de tickets y misiones completadas.

---

## 5. Requisitos Flutter (Sugeridos para Paridad)
- **State Management:** `Provider` o `Bloc`.
- **UI:** `Google Fonts (Inter)`, `FlChart` (para los gráficos), `FontAwesomeIcons`.
- **Backend Communication:** `Http` o `Dio` conectando a un Backend API (Java/Node) o directamente vía `PostgreSQL Client` si es interna.
- **Multimedia:** `path_provider`, `camera`, `record` (para mensajes de voz).
