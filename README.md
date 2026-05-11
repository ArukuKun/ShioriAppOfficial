ShioriApp 📖
ShioriApp es un lector de manga moderno para Android construido con Jetpack Compose. La aplicación permite a los usuarios explorar, organizar y leer contenido de múltiples fuentes externas mediante un sistema de extensiones dinámico inspirado en proyectos como Tachiyomi y Mihon.

✨ Características Principales
Búsqueda Global Multi-Fuente: Realiza búsquedas asíncronas en tiempo real a través de todas las extensiones instaladas. Los resultados se agrupan por fuente y cuentan con un sistema de debounce para optimizar las peticiones mientras escribes.

Biblioteca Local: Sistema de favoritos persistente que permite guardar y gestionar mangas localmente. Incluye lógica de "autocuración" para manejar datos de fuentes obsoletas.

Lector Inmersivo: Experiencia de lectura fluida con:

Modo Inmersivo: Ocultación automática de las barras del sistema.

Carga Inteligente: Prefetch de páginas y capítulos para un scroll infinito sin interrupciones.

Transiciones de Capítulo: Divisores visuales que indican el fin de un capítulo y el inicio del siguiente.

Gestión de Extensiones: Capacidad para cargar y utilizar fuentes de terceros de forma dinámica.

Exploración por Categorías: Filtros rápidos para descubrir contenido nuevo basados en géneros y etiquetas.

🛠️ Stack Tecnológico
Lenguaje: Kotlin.

UI: Jetpack Compose (Material 3).

Arquitectura: MVVM (Model-View-ViewModel).

Concurrencia: Kotlin Coroutines & Flow.

Navegación: Compose Navigation con paso de argumentos complejos y rutas seguras.

Imágenes: Coil para la carga asíncrona de portadas y páginas de manga.

Persistencia: SharedPreferences con serialización JSON para la biblioteca.

📂 Estructura del Proyecto
core.util: Utilidades para la carga dinámica de extensiones (ExtensionLoader).

data: Repositorios y fuentes de datos (Keiyoushi, LibraryManager).

domain.model: Modelos de datos centrales como MangaInfo, ChapterInfo y PageInfo.

screens: Pantallas principales (Home, Explore, Search, Details, Reader).

viewmodel: Lógica de negocio y gestión de estado para cada pantalla.

🚀 Últimos Cambios (Resumen de Commit)
feat: sistema de biblioteca local, búsqueda global asíncrona y correcciones de UI
