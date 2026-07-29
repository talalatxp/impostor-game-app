# El Impostor

Aplicación Android local para jugar a descubrir al impostor compartiendo un único teléfono.

## Tecnología y arquitectura

- Kotlin y Jetpack Compose (Material 3).
- MVVM: cada pantalla observa `GameViewModel` y expresa eventos de usuario.
- Clean Architecture: `domain` contiene modelos, contratos y casos de uso; `data` implementa el repositorio local; `presentation` contiene la UI.
- DataStore Preferences con serialización JSON para persistir categorías, palabras, jugadores, ajustes y estadísticas entre sesiones.

## Funcionalidades

- Configuración de jugadores, impostores, temporizador, pista para el impostor y categorías activas.
- Reparto secreto mediante tarjeta que se revela al mantener pulsado y deslizar.
- Temporizador de debate con pausa y suma de 30 segundos.
- Resultados, registro de victorias y nueva ronda.
- Categorías predeterminadas y personalizadas; creación, renombrado, eliminación y edición de palabras y pistas.

## Abrir el proyecto

1. Abre esta carpeta desde Android Studio.
2. Selecciona JDK 17 o superior y sincroniza Gradle.
3. Ejecuta la configuración `app` en un emulador o dispositivo Android (API 26+).

La aplicación no necesita servidor ni cuenta: todos los datos se guardan localmente en el dispositivo.
