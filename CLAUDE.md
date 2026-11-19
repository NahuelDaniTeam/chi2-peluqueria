# CHI2 · Sistema de turnos para barberías y peluquerías

Sistema que CHI2 vende a barberías y peluquerías para manejar sus turnos. Es uno de los sistemas
vendibles de la marca: cada uno vive en su propio repo, y la web principal (repo `chi2`) presenta
los sistemas y lleva a cada cliente al suyo con un click.

**Estado:** estructura base (etapa 0). Alcance de la v1 en borrador (§3).
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
1. Barberías, usuarios y acceso
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

- `pagina`: controladores de pantallas Thymeleaf.
- `api`: controladores REST bajo `/api`.
- Cada funcionalidad nueva va en su propio paquete (ej. `turno`, `negocio`), con su entidad,
  repositorio, servicio y controladores.

## 5. Cómo correrlo

Requisitos: JDK 21 y Docker Desktop abierto (Postgres corre en un contenedor).

```bash
./mvnw spring-boot:run   # levanta la app y el Postgres de compose.yaml → http://localhost:8080
./mvnw test              # corre los tests (los que usan la base levantan Postgres con Testcontainers)
npm run watch:css        # opcional: recompila Tailwind en vivo mientras se editan plantillas
```

## 6. Equipo y forma de trabajo

Trabajamos dos personas en paralelo, cada una con su propio Claude. Estas reglas aplican
a cualquier sesión de Claude en este repo, sea de quien sea (Nahuel o Daniel).

### Ramas

- `main` es la rama estable. **Nunca se commitea ni se pushea directo a `main`.**
- Cada uno trabaja en su rama: `<usuario-github>/<tema>` (ej: `NahuelLuna/login`).
- Ramas cortas y enfocadas en una sola cosa. Mejor varias PRs chicas que una gigante.
- Todo entra a `main` por Pull Request (squash merge). Borrar la rama después del merge.

### Mantenerse sincronizado

- Antes de arrancar una tarea y antes de abrir/actualizar una PR:
  `git fetch origin && git rebase origin/main`.
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

- Commits chicos y descriptivos, en español o inglés pero consistentes dentro de una PR.
- Formato sugerido: `tipo: descripción` (`feat`, `fix`, `refactor`, `docs`, `chore`, `test`).
- Las migraciones de Flyway ya mergeadas a `main` no se editan: se agrega una nueva.

### Worktrees (Orca)

- Trabajamos con git worktrees: el stack de `git stash` es compartido entre todos los
  worktrees. No usar `git stash` / `git stash pop` a secas; preferir un commit WIP temporal.
