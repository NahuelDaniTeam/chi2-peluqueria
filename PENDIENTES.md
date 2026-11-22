# Pendientes · Sistema de turnos (peluquería)

Hoja compartida de tareas pendientes entre Nahuel y Daniel para este repo. Claude la lee al arrancar cada
sesión y la menciona en cada respuesta (ver `CLAUDE.md` §7). Los pendientes de la web principal están en el
`PENDIENTES.md` del repo `chi2`, no acá.

**Cómo se escribe un pendiente:** tiene que poder entenderlo alguien que no estuvo en la charla donde surgió.
Cada ítem lleva:
- **Contexto:** por qué existe y qué ya está decidido.
- **Qué entregar:** concreto y verificable.
- **Dónde:** el archivo o la sección donde va el resultado.
- La línea `cargado por <quien>, <AAAA-MM-DD>`.

Los ítems se numeran en orden de carga y el número no se reusa. Al completar un ítem se mueve a **Hechos**
con la fecha. Los hechos de más de un mes se pueden borrar.

**Documento de referencia:** `docs/01-vision-alcance.md` (qué entra en la v1, preguntas abiertas en §9).

---

## Para Daniel

_(nada por ahora)_

## Para Nahuel

### 3. Link "Ingresar" desde la web principal
- **Contexto:** `docs/01-vision-alcance.md` §9.2: el botón "Ingresar" de la web de CHI2 está fuera de la v1
  de esa web, pero los clientes necesitan una forma de llegar a este sistema. Nahuel es quien lleva la web
  principal.
- **Qué entregar:** al menos un link visible en la web de CHI2 que lleve al login de este sistema (la URL de
  producción todavía no está definida; mientras tanto, dejarlo preparado con la ruta `/login`).
- **Dónde:** repo `chi2` (cargarlo también en su `PENDIENTES.md` si se trabaja allá). Cerrar la pregunta
  §9.2 en `docs/01-vision-alcance.md` de este repo.
- cargado por Nahuel, 2026-11-19

## Para los dos

### 2. Acordar el flujo de git de este repo
- **Contexto:** antes, `CLAUDE.md` §6 decía que todo entraba a `main` por Pull Request desde ramas
  `<usuario-github>/<tema>`. Daniel propuso otro flujo: cada uno en su rama personal, se sube a `main` con
  fast-forward solo cuando el dueño lo pide, sin Pull Requests. Desde el 2026-11-19 la propuesta de Daniel
  **ya está en `main`** (entró junto con las etapas 2 y 3), así que es la regla vigente; falta que los dos
  la confirmen o la ajusten (por ejemplo, el nombre de la rama personal de Nahuel).
- **Qué entregar:** la confirmación de los dos, y `CLAUDE.md` §6 ajustado si cambia algo.
- **Dónde:** `CLAUDE.md` §6.
- cargado por Nahuel, 2026-11-19

### 4. Decidir cómo queda una reserva hecha desde el link
- **Contexto:** `docs/01-vision-alcance.md` §9.1. La propuesta es que el turno quede **reservado directo**
  (el horario se bloquea al instante y, si era falso, la barbería lo cancela). La alternativa es que quede
  **pendiente** hasta que la barbería lo confirme. Afecta a la etapa 6 (reserva online) y a cómo se ve la
  agenda.
- **Qué entregar:** la opción elegida y el porqué, en una o dos frases.
- **Dónde:** `docs/01-vision-alcance.md` §9.1 (pasarla a decisión tomada).
- cargado por Nahuel, 2026-11-19

## Hechos

- **5. Reubicar la rama `dani` sobre `main`** (Daniel): `dani` se puso igual a `origin/main` (no tenía
  trabajo sin subir) y se pusheó con `--force-with-lease`. La etapa 4 arranca desde ahí. · 2026-11-22
- **1. Subir a `main` las etapas 2 y 3** (Daniel): subidas por Nahuel el 2026-11-19, rebaseadas sobre
  `main`, con los 105 tests pasando. Ver pendiente 5 para reubicar la rama `dani`.
