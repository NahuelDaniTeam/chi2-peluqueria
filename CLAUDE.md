# CHI2 · Sistema de turnos para barberías y peluquerías

Sistema que CHI2 vende a barberías y peluquerías para manejar sus turnos. Es uno de los sistemas
vendibles de la marca: cada uno vive en su propio repo, y la web principal (repo `chi2`) presenta
los sistemas y lleva a cada cliente al suyo con un click.

**Estado:** etapa 1 (barberías, usuarios y acceso) hecha. Sigue la etapa 2 (§3).
**Última actualización:** 2026-11-18

---

## 1. Contexto

- **Marca:** CHI2 (Nahuel Luna y Daniel Patolsky).
- **Web principal:** repo `chi2`. Es el sitio público y el medio de contacto con clientes potenciales.
  Enlaza a este sistema; no comparte código ni base de datos con él.
- **Otros sistemas (repos aparte):** agenda tipo kanban, y a futuro gimnasio, veterinaria, etc.
- **Meta:** vender la primera versión a barberías en aproximadamente dos meses (sin fecha fija).
- **Presupuesto:** hasta USD 10 por mes entre dominio y hosting (compartido con la web principal).

## 2. Decisiones tomadas

- **Un solo sistema para muchas barberías (multi-negocio):** un único deploy y una única base de datos.
  Cada barbería es un *negocio* con su cuenta y su link propio (ej. `turnos.chi2.com/barberia-juan`).
  Todos los datos de un negocio quedan separados de los de los demás.
- **Sin pagos dentro del sistema por ahora** (ni señas ni cobros online).
- **Stack igual al de la web principal** (§4), con las mismas reglas de trabajo (§6).
- **El backend se diseña como API REST desde el inicio**, para que una app nativa futura consuma los
  mismos endpoints.

## 3. Alcance y etapas

El detalle está en [`docs/01-vision-alcance.md`](docs/01-vision-alcance.md) (borrador, con preguntas abiertas).
Se construye por etapas, una por vez:

0. Base del proyecto ✅
1. Barberías, usuarios y acceso ✅
2. Servicios, barberos y horarios
3. Agenda y disponibilidad
4. Atención "tipo Uber" (empezar / finalizar, medio de pago, ausente, modificar, sin turno)
5. Métricas por barbero y del negocio
6. Reserva online con aviso por WhatsApp (link `wa.me`, gratis)

**v1 vendible:** etapas 1 a 6. **Fuera de la v1 (ya decidido):** pagos y señas online, comisiones,
API de WhatsApp.

**Prioridad de diseño:** se usa sobre todo desde el celular. Cada pantalla se diseña primero para
celular y tiene que ser muy intuitiva (acciones en uno o dos toques); la computadora es secundaria.

## 4. Stack técnico

- **Backend:** Java 21 con Spring Boot
- **Plantillas:** Thymeleaf
- **Estilos:** Tailwind CSS (se compila en el build de Maven, ver `package.json`)
- **Marca:** la misma de la web principal. Los tokens de `src/main/frontend/main.css`, las fuentes de
  `fragments/layout.html`, el isotipo χ² y `static/favicon.svg` están copiados del repo `chi2` (todavía
  provisorios, ver su `PENDIENTES.md` 10): si allá cambian, se actualizan acá también.
- **Base de datos:** PostgreSQL, con migraciones Flyway (`src/main/resources/db/migration`)
- **Pruebas:** JUnit 5 y Testcontainers
- **Build:** Maven (wrapper `mvnw`)
- **Hora:** la app corre en UTC; los turnos se guardan en UTC y se muestran en la hora de cada barbería.

### Estructura de paquetes (`com.chi2.peluqueria`)

- `pagina`: login y pantalla de inicio.
- `seguridad`: login, roles, "mantener la sesión iniciada" y el usuario de CHI2 inicial.
- `negocio`: barberías (alta, suspensión) y su link (`Slugs`).
- `usuario`: usuarios, equipo de cada barbería y "Mi cuenta".
- `comun`: excepciones compartidas (`NoEncontradoException` → 404, `OperacionNoPermitidaException` → 400,
  `DatoNoDisponibleException` → error al lado del campo del formulario).
- `api`: controladores REST bajo `/api`.
- Cada funcionalidad nueva va en su propio paquete (ej. `servicio`, `turno`), con su entidad,
  repositorio, servicio y controladores.

### Roles y acceso

| Rol | Entra a | Ve |
|---|---|---|
| `ADMIN` (CHI2) | `/admin/**` | Todas las barberías. No pertenece a ninguna. |
| `DUENO` | `/equipo/**` y todo lo de su barbería | Todo su negocio. |
| `BARBERO` | Lo común (`/`, `/cuenta`) | Solo lo suyo. |

Las reglas por ruta están en `seguridad/SeguridadConfig`. Toda ruta nueva requiere sesión por defecto.

### Aislamiento entre barberías (regla para todo el código)

- Toda tabla con datos de una barbería lleva `negocio_id`.
- Los controladores reciben el usuario con `@AuthenticationPrincipal UsuarioAutenticado` y pasan
  `getNegocioId()` al servicio. **Nunca** se busca un dato de una barbería solo por su id: siempre por
  id **y** negocio (ej. `findByIdAndNegocioId`). Si no aparece, `NoEncontradoException` (404), sin
  distinguir "no existe" de "es de otra barbería".
- Cada funcionalidad nueva suma un test que pruebe que una barbería no ve ni toca datos de otra
  (ver `usuario/EquipoTest`).

### Tests

Los tests de punta a punta extienden `PruebaIntegracion` (app completa + Postgres real + MockMvc, cada
test en una transacción que se deshace) y arman datos con `DatosDePrueba`. Todos comparten la misma
configuración para levantar un solo contenedor.

## 5. Cómo correrlo

Requisitos: JDK 21 y Docker Desktop abierto (Postgres corre en un contenedor).

```bash
./mvnw spring-boot:run   # levanta la app y el Postgres de compose.yaml → http://localhost:8080
./mvnw test              # corre los tests (los que usan la base levantan Postgres con Testcontainers)
npm run watch:css        # opcional: recompila Tailwind en vivo mientras se editan plantillas
```

**Primer usuario:** al arrancar se crea el usuario de CHI2 si están las variables `CHI2_ADMIN_EMAIL` y
`CHI2_ADMIN_CONTRASENA`. Con él se entra a `/admin/negocios` y se dan de alta las barberías. En PowerShell:

```powershell
$env:CHI2_ADMIN_EMAIL = "admin@chi2.local"; $env:CHI2_ADMIN_CONTRASENA = "una-clave-larga"; ./mvnw spring-boot:run
```

**En producción** además hay que definir `CHI2_CLAVE_RECORDARME` (una clave larga y secreta, propia del
servidor): firma la cookie de "mantener la sesión iniciada".

## 6. Equipo y forma de trabajo

Trabajamos dos personas en paralelo, cada una con su propio Claude. Estas reglas aplican
a cualquier sesión de Claude en este repo, sea de quien sea (Nahuel o Daniel).

### Ramas (en este repo es distinto que en `chi2`: sin Pull Requests)

- `main` es la rama estable, la que el compañero pullea.
- Cada uno trabaja en **su rama personal** (Daniel: `dani`). Claude commitea ahí y la pushea a
  `origin` para que quede respaldada.
- **A `main` se sube solo cuando el dueño de la rama lo pide.** Se hace con fast-forward
  (`git push origin dani:main`), sin commits de merge. Si `origin/main` avanzó, primero se rebasea la
  rama sobre `origin/main` y se corren los tests.
- No se crean ramas extra en GitHub ni Pull Requests.

### Mantenerse sincronizado

- Antes de arrancar una tarea: `git fetch origin && git rebase origin/main`.
- Integrar seguido (al menos una vez por sesión) para que los conflictos sean chicos.
- En la rama propia se puede reescribir historia, pero pushear siempre con
  `git push --force-with-lease`, nunca `--force` a secas. Jamás force-push a `main`.

### Resolución de conflictos (regla para Claude)

Cuando un rebase, merge o pull genera conflictos, **Claude los resuelve solo, sin frenar
a preguntar**, siguiendo este proceso:

1. Entender la intención de cada lado antes de tocar nada: leer los commits involucrados
   (`git log origin/main..HEAD`, `git log HEAD..origin/main`, `git show <sha>`) y el código
   alrededor del conflicto.
2. Resolver preservando **las dos** funcionalidades. Nunca descartar en silencio el trabajo
   del compañero (ni el propio) eligiendo "ours"/"theirs" de un archivo entero.
3. Archivos generados (lockfiles, builds, snapshots): no mergear a mano; regenerarlos con
   la herramienta correspondiente después de resolver el resto.
4. Verificar después de resolver: que compile/levante y que pasen los tests.
5. Al terminar, contarle al usuario en un resumen corto qué conflictos hubo y cómo se
   resolvió cada uno.

**Única excepción — preguntar antes de resolver:** si los dos lados cambian el mismo
comportamiento de formas contradictorias (no se pueden conservar ambos), Claude explica
las dos opciones y le pregunta al usuario cuál va.

### Commits

- Commits chicos y descriptivos, en español.
- Formato sugerido: `tipo: descripción` (`feat`, `fix`, `refactor`, `docs`, `chore`, `test`).
- Las migraciones de Flyway ya mergeadas a `main` no se editan: se agrega una nueva.

### Worktrees (Orca)

- Trabajamos con git worktrees: el stack de `git stash` es compartido entre todos los
  worktrees. No usar `git stash` / `git stash pop` a secas; preferir un commit WIP temporal.
