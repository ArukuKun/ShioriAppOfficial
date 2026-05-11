# 📖 ShioriApp

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)
![MVVM](https://img.shields.io/badge/Architecture-MVVM-FFA000?logo=android&logoColor=white)

**ShioriApp** es una aplicación de lectura de manga de código abierto para Android, diseñada para ofrecer una experiencia fluida, moderna y centralizada. La aplicación utiliza un potente sistema de carga de extensiones que permite acceder a múltiples fuentes de contenido desde una única interfaz.

---

## ✨ Características Principales

* **🔍 Búsqueda Global Asíncrona:** Realiza búsquedas simultáneas en todas las extensiones instaladas sin bloquear la interfaz de usuario.
* **⌨️ Búsqueda Inteligente (Debounce):** Resultados en tiempo real mientras escribes, optimizando el tráfico de red mediante un retardo controlado.
* **📚 Gestión de Biblioteca:** Añade y elimina mangas de tu biblioteca personal con persistencia local de datos.
* **📖 Lector Inmersivo:** Disfruta de un modo de lectura que aprovecha toda la pantalla (Immersive Mode), ocultando las barras del sistema para evitar distracciones.
* **🧩 Extensiones Dinámicas:** Sistema robusto para cargar extensiones externas y fuentes de manga de manera dinámica.
* **🎨 Diseño Material 3:** Interfaz construida íntegramente con Jetpack Compose siguiendo los estándares de diseño más modernos.

---

## 🛠️ Tecnologías y Librerías

* **Lenguaje:** [Kotlin](https://kotlinlang.org/)
* **Framework UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
* **Navegación:** [Compose Navigation](https://developer.android.com/jetpack/compose/navigation)
* **Arquitectura:** MVVM con Corrutinas de Kotlin y StateFlow para la gestión reactiva del estado
* **Carga de Imágenes:** [Coil](https://coil-kt.github.io/coil/) para el manejo eficiente de portadas y páginas de manga
* **Red:** [OkHttp](https://square.github.io/okhttp/) y extensiones personalizadas para peticiones seguras
* **Parsing:** [Jsoup](https://jsoup.org/) para el procesamiento de contenido HTML de las fuentes

---

## 🚀 Instalación

Sigue estos pasos para compilar y ejecutar el proyecto en tu entorno local:

1.  **Clona el repositorio:**
    ```bash
    git clone [https://github.com/tu-usuario/shioriappofficial.git](https://github.com/tu-usuario/shioriappofficial.git)
    ```
2.  **Abre el proyecto** en Android Studio (Ladybug o superior recomendado).
3.  **Sincroniza el proyecto** con los archivos Gradle (`build.gradle.kts`).
4.  **Ejecuta la aplicación** en un emulador o dispositivo físico con Android 8.0 (API 26) o superior.

---

## 📁 Estructura del Proyecto

El proyecto sigue una organización modular por capas para facilitar el mantenimiento:

* `core.util`: Contiene las clases de carga dinámica de extensiones y utilidades generales.
* `domain.model`: Define los modelos de datos base como `MangaInfo`, `ChapterInfo` y `PageInfo`.
* `screens`: Contiene las interfaces de usuario (Composables) para cada sección (Home, Search, Details, Reader).
* `viewmodel`: Maneja la lógica de negocio y el estado de las pantallas.
* `navigation`: Define el grafo de navegación y las rutas de la aplicación.

---

## 🤝 Contribuciones

¡Las contribuciones son bienvenidas! Si tienes ideas para nuevas funciones o has encontrado algún error, no dudes en abrir un *Issue* o enviar un *Pull Request*.

---

## 📄 Licencia

Este proyecto está bajo la licencia **MIT**. Consulta el archivo `LICENSE` para más detalles.

---
*Desarrollado con ❤️ para la comunidad de lectores de manga.*
