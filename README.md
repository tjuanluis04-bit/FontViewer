# Font Viewer

App Android nativa (Kotlin + Jetpack Compose) para explorar las tipografías
(`.ttf` / `.otf`) que tengas en una carpeta del teléfono y previsualizar un
texto de prueba con cada una.

## Cómo funciona
1. Al abrir la app, tocás **"Elegir carpeta de fuentes"** y seleccionás la
   carpeta donde tenés tus `.ttf`/`.otf` (por ejemplo `Download` o `Fonts`).
   Esto usa el selector nativo de Android (Storage Access Framework), así que
   no hace falta pedir permisos de almacenamiento.
2. La app lista los archivos de fuente encontrados.
3. Escribís el texto que quieras en el campo de arriba.
4. Tocás una tipografía de la lista y ves el texto renderizado con esa fuente.
5. La carpeta elegida se recuerda entre sesiones.

## Crear el repositorio en GitHub desde cero

1. Instalá git si no lo tenés, y and abrí una terminal en esta carpeta
   (`FontViewer/`).
2. Iniciá el repo local:
   ```bash
   git init
   git add .
   git commit -m "Proyecto inicial: Font Viewer"
   ```
3. Andá a https://github.com/new, elegí un nombre (ej. `font-viewer`),
   dejalo **público o privado** (ambos funcionan con Actions) y **no** marques
   "Add a README" (ya tenés uno). Creá el repositorio.
4. GitHub te va a mostrar comandos para conectar tu repo local. Usá algo así
   (reemplazá `TU-USUARIO` y `font-viewer`):
   ```bash
   git remote add origin https://github.com/TU-USUARIO/font-viewer.git
   git branch -M main
   git push -u origin main
   ```

## Compilar el APK automáticamente

En cuanto hagas el `push` a la rama `main`, el workflow en
`.github/workflows/build-apk.yml` se dispara solo. Para verlo:

1. Entrá a tu repositorio en GitHub → pestaña **Actions**.
2. Vas a ver el workflow **"Build APK"** corriendo (tarda 2-4 minutos).
3. Cuando termine (tilde verde), entrá al workflow → abajo vas a ver
   **Artifacts** → descargá `FontViewer-debug-apk`.
4. Eso descarga un `.zip` que contiene `app-debug.apk`.

También podés lanzarlo manualmente sin hacer push: en la pestaña **Actions**,
elegí "Build APK" → **Run workflow**.

## Instalar el APK en tu teléfono

1. Pasá el archivo `app-debug.apk` a tu teléfono (por USB, Drive, WhatsApp,
   etc.).
2. Abrilo desde el explorador de archivos del teléfono.
3. Si Android te avisa que no permite instalar apps de "origen desconocido",
   andá a la configuración que te propone y habilitalo solo para esa app o
   ese origen.
4. Instalá y abrí la app.

## Notas técnicas
- `minSdk` 26 (Android 8.0) porque se usa `Typeface.Builder(FileDescriptor)`
  para cargar tipografías directamente desde el Storage Access Framework sin
  copiar archivos.
- No pide permisos de almacenamiento amplios: solo tiene acceso a la carpeta
  puntual que elegís (persistido con `takePersistableUriPermission`).
- Si querés editar el proyecto en Android Studio, abrilo como proyecto
  existente (`Open` → seleccioná la carpeta `FontViewer`) y Android Studio va
  a generar automáticamente el Gradle Wrapper local (por eso no se incluye en
  el repo, ya que son binarios).
