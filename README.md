<p align="center">
  <img src="docs/icon-512.png" width="120" alt="Seguimiento de Precios" />
</p>

<h1 align="center">Seguimiento de Precios</h1>

<p align="center">
  App multiplataforma (Android + Windows) para <b>rastrear precios</b>, comparar tiendas
  y analizar tus gastos. Offline-first, modo oscuro, con graficos de evolucion de precios.
</p>

<p align="center">
  <a href="https://github.com/jhonsu01/SeguimientoPrecios/releases/latest"><img src="https://img.shields.io/github/v/release/jhonsu01/SeguimientoPrecios?label=descarga&color=14B8A6" alt="Release"></a>
  <img src="https://img.shields.io/badge/Android-APK-3DDC84?logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/Windows-MSI-0078D6?logo=windows&logoColor=white" alt="Windows">
  <img src="https://img.shields.io/badge/licencia-MIT-blue" alt="MIT">
</p>

---

## Descargas

Los binarios se publican automaticamente en la [**ultima release**](https://github.com/jhonsu01/SeguimientoPrecios/releases/latest):

| Plataforma | Archivo | Instalacion |
| ---------- | ------- | ----------- |
| Android    | `SeguimientoPrecios-vX.Y.Z.apk` | Descarga e instala (permite "instalar apps de origen desconocido"). |
| Windows    | `SeguimientoPrecios-vX.Y.Z.msi` | Doble clic e instala. |

> Cada version conserva **solo la ultima release**; los binarios llevan el tag en su nombre.

---

## Funcionalidades (v0.2.0)

- **CRUD de productos** con categoria, tipo/marca, unidad de medida y codigo de barras.
- **Registro historico de precios** por producto, tienda y tipo (unitario/promocion).
- **Graficos de evolucion** del precio en el tiempo (dibujados a medida, sin dependencias pesadas).
- **Indicadores de tendencia** (sube / baja / estable) y estadisticas min/promedio/maximo.
- **Comparacion entre tiendas**: ranking de la tienda mas economica por producto.
- **IA predictiva de precios**: proyeccion a ~7 dias por regresion lineal.
- **OCR de facturas con OpenAI**: extrae productos y precios de una foto (API key del usuario).
- **Mi Alacena**: inventario domestico con alertas y lista de compras automatica.
- **PIN de acceso** con hash SHA-256.
- **Importar / Exportar** toda la base de datos en ZIP (SQLite + JSON).
- **Base de datos local SQLite** (offline-first): Room en Android, sql.js en Windows.
- **UI moderna en modo oscuro** con identidad de marca compartida.
- **Iconos adaptativos** multi-densidad (Android) y `.ico` multi-tamano (Windows).

### Roadmap (proximas versiones)

Escaneo de codigo de barras · sincronizacion opcional en la nube · notificaciones de ofertas.

### OCR: como obtener tu API key de OpenAI

El OCR usa tu propia API key (se guarda **solo en tu dispositivo**, en Ajustes). Generala en
https://platform.openai.com/api-keys y pegala en la app (Ajustes → OCR de facturas).

---

## Arquitectura

```
SeguimientoPrecios/
├─ android-app/     Kotlin + Jetpack Compose + Room (SQLite)   -> APK
├─ windows-app/     Electron + sql.js (SQLite WASM)            -> MSI
├─ docs/            Recursos e iconos
└─ .github/workflows/  CI (build) + Release (publicacion auto)
```

Ambas apps comparten el mismo modelo de datos (Productos, Precios, Tiendas) y la misma
identidad visual, pero son nativas a cada plataforma.

---

## Compilar desde el codigo

### Requisitos

- **Android:** JDK 17+, Android SDK (API 35). El wrapper de Gradle se incluye.
- **Windows:** Node.js 18+ y npm.

### Android (APK)

```bash
cd android-app
# APK de depuracion (instalable directamente):
./gradlew assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk

# APK de release firmado (requiere keystore via variables de entorno):
./gradlew assembleRelease -PappVersionName=0.1.0 -PappVersionCode=1
```

Variables de entorno para firmar el release: `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD`. Si no se proveen, el release se firma con la clave de debug.

### Windows (MSI)

```bash
cd windows-app
npm install
npm run start      # ejecutar en modo desarrollo
npm run dist:win   # generar el instalador MSI en windows-app/dist/
```

### Regenerar iconos

```bash
python execution/generate_icons.py   # requiere Pillow
```

---

## Publicacion automatica (CI/CD)

- **`CI`** (push a `main`): compila el APK de debug y empaqueta la app de Windows para
  verificar que todo construye.
- **`Release`** (push de un tag `vX.Y.Z`): compila el APK firmado y el MSI, nombra los
  binarios con el tag, publica la release y **borra las releases anteriores** para conservar
  solo la ultima.

Publicar una nueva version:

```bash
git tag v0.2.0
git push origin v0.2.0
```

---

## Licencia

MIT © jhonsu01
