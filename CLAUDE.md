# CHI2 · Sistema de turnos para barberías y peluquerías

Sistema que CHI2 vende a barberías y peluquerías para manejar sus turnos. Es uno de los sistemas
vendibles de la marca: cada uno vive en su propio repo, y la web principal (repo `chi2`) presenta
los sistemas y lleva a cada cliente al suyo con un click.

**Estado:** etapas 1 a 4 hechas. Sigue la etapa 5, métricas (§3).
**Última actualización:** 2026-11-22

> **Pendientes del equipo:** en [`PENDIENTES.md`](PENDIENTES.md). Claude los muestra al arrancar cada
> sesión y los menciona en cada respuesta (§7).

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
2. Servicios, barberos y horarios ✅
3. Agenda y disponibilidad ✅
4. Atención "tipo Uber" (empezar / finalizar, medio de pago, ausente, modificar, sin turno) ✅
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

- `pagina`: login, a dónde va cada rol al entrar y la "puesta en marcha" del dueño (`PuestaEnMarcha`).
- `seguridad`: login, roles, "mantener la sesión iniciada" y el usuario de CHI2 inicial.
- `negocio`: barberías (alta, suspensión) y su link (`Slugs`).
- `usuario`: usuarios, equipo de cada barbería y "Mi cuenta".
- `servicio`: catálogo de servicios de cada barbería (duración en minutos, precio en pesos sin centavos).
- `barbero`: ficha de cada miembro: si atiende (`usuario.atiende`), qué servicios hace y precio especial.
  Escucha los eventos `ServicioCreado` y `MiembroAgregado` para asignar servicios de entrada.
- `horario`: horario semanal (`franja_horaria`, hasta dos turnos por día) y días sin atender
  (`ausencia`). En las dos tablas, `usuario_id` nulo = del local. Un barbero sin franjas propias
  trabaja en el horario del local; con franjas propias, trabaja en esas franjas dentro del horario del local.
- `turno`: la agenda (`/agenda/**`). Turnos (`turno`, en UTC), su historial (`turno_cambio`: qué pasó,
  quién y cuándo) y el cálculo de horarios libres (`Disponibilidad`, con `Tramos` en minutos del día).
  La base impide que un barbero tenga dos turnos superpuestos (`EXCLUDE` con `btree_gist`, ver
  `V6__turnos.sql` y `V7__atencion.sql`); los estados que ocupan lugar están en `EstadoTurno.ocupaLugar()` y
  en esa restricción, y si cambia uno hay que cambiar el otro. La atención sin turno (`turno.sin_turno`)
  queda afuera de la restricción, pero `Disponibilidad` igual la cuenta como ocupada. Escucha
  `SaleDeLaAgenda` para que nadie se dé de baja ni deje de atender con turnos reservados.
  La atención "tipo Uber" está en `AtencionService` / `AtencionController`: empezar, finalizar con el cobro
  (`monto` en pesos y `medio_pago`, solo marcados), "no vino", deshacer, corregir el cobro y sin turno. Qué
  se puede hacer con un turno en cada momento lo deciden los métodos `puede...()` de `Turno`.
- `comun`: excepciones compartidas (`NoEncontradoException` → 404, `OperacionNoPermitidaException` → 400,
  `DatoNoDisponibleException` → error al lado del campo del formulario, `AccionBloqueadaException` →
  aviso arriba de la pantalla) y `Formatos` (precios, duraciones, fechas y horas a la argentina).
- `api`: controladores REST bajo `/api`.
- Cada funcionalidad nueva va en su propio paquete (ej. `servicio`, `turno`), con su entidad,
  repositorio, servicio y controladores.

### Roles y acceso

| Rol | Entra a | Ve |
|---|---|---|
| `ADMIN` (CHI2) | `/admin/**` | Todas las barberías. No pertenece a ninguna. |
| `DUENO` | `/equipo/**`, `/barberia/**`, `/agenda/**` y `/cuenta` | Todo su negocio. |
| `BARBERO` | `/agenda/**` y `/cuenta` | Solo lo suyo. |

Las reglas por ruta están en `seguridad/SeguridadConfig`. Toda ruta nueva requiere sesión por defecto.
En las rutas que comparten dueño y barbero (como `/agenda/**`), el servicio recibe `soloDe`: `null` para
el dueño, el id del barbero para un barbero, que solo ve y toca lo suyo (lo demás le da 404).

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

**Puerto:** la web principal (repo `chi2`) también usa el 8080. Si las dos corren a la vez, levantar esta en
otro puerto sumando `$env:SERVER_PORT = "8081";` antes de `./mvnw spring-boot:run`.

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

## 7. Pendientes (`PENDIENTES.md`)

Los pendientes del equipo para este repo se registran **solo** en `PENDIENTES.md`, en la raíz, con
secciones **"Para Nahuel"**, **"Para Daniel"**, **"Para los dos"** y **"Hechos"**. No se duplican en otros
documentos: esos marcan `[PENDIENTE]` en el lugar y la tarea se anota en la hoja. Es la misma forma de
trabajo que en el repo `chi2`, pero acá además se mencionan en **cada** respuesta.

**Reglas para Claude:**

1. **Identificar con quién hablás:** usá `git config user.name`. `NahuelLuna` es Nahuel y
   `DanielPatolsky` es Daniel.
2. **Al arrancar cada sesión**, antes de responder lo primero que te pidan, leé `PENDIENTES.md` (la versión
   de `origin/main` si la rama local está atrasada) y mostrale al usuario en una lista corta:
   - lo que tiene pendiente en su sección y en **"Para los dos"**;
   - cuántos pendientes tiene el compañero, sin el detalle salvo que lo pida.
3. **En cada respuesta** (no solo la primera de la sesión), cerrá con una línea corta sobre los pendientes,
   por ejemplo: `Pendientes: tenés 2 (n.º 3 link "Ingresar", n.º 4 reserva desde el link) · Daniel: 1`.
   Si lo que se acaba de hacer tiene que ver con un pendiente, decilo en esa línea. Si el usuario no tiene
   ninguno, decí igual "Pendientes: no tenés ninguno · <compañero>: N".
4. **Al terminar una tarea**, si cumple un pendiente, movelo a **Hechos** con la fecha. Si surge algo nuevo
   que tiene que hacer el compañero o el usuario, cargalo en la sección correcta con el formato de la hoja
   (contexto, qué entregar, dónde), **escrito para alguien que no estuvo en la conversación**. Avisale al
   usuario qué agregaste o cerraste.
5. **Cargar y cerrar pendientes** se hace en la misma rama que el trabajo que los genera o resuelve.
6. Si el usuario pide "dejale un pendiente a Daniel" (o a Nahuel), se agrega en la sección del otro con
   `cargado por <quien>, <fecha>`.
