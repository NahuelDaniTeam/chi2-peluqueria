# Visión y alcance · Sistema de turnos para barberías

**Estado:** borrador v0.5, con preguntas abiertas (§9)
**Última actualización:** 2026-11-22
**Autor:** Daniel Patolsky

---

## 1. Problema

Las barberías arreglan los turnos por WhatsApp a mano: el dueño o los barberos contestan mensajes y
anotan en un cuaderno o en la agenda del celular. Se pisan turnos, se pierden mensajes, y nadie sabe
con certeza cuántos cortes hizo cada barbero ni cuánto se cobró en efectivo y cuánto por transferencia.

## 2. Visión

Un sistema simple, pensado para el celular, donde:

- el cliente entra al link de la barbería, elige servicio, barbero (o "cualquiera"), día y horario,
  y la reserva le llega a la barbería por WhatsApp, sin costo;
- cada barbero ve sus turnos y los marca como empezados y terminados, como un chofer de Uber marca un viaje;
- todo lo que se hace y se cobra queda registrado, y cada barbero (y el dueño) ve sus números.

La barbería entra al sistema desde la web principal de CHI2 (repo `chi2`), con un click.

## 3. Usuarios y roles

| Rol | Quién es | Qué hace |
|---|---|---|
| **Cliente** | La persona que se corta | Reserva desde el link de la barbería. No tiene cuenta: se identifica con su nombre y teléfono. |
| **Barbero** | Trabaja en la barbería | Ve su agenda, atiende (empezar / finalizar), marca cómo se cobró y ve **solo lo suyo**. |
| **Dueño o encargado** | Administra la barbería (puede ser también barbero) | Configura la barbería y ve **todo el negocio**: agenda y métricas de todos. |
| **CHI2** | Nosotros | Da de alta las barberías nuevas. |

## 4. Funcionalidades

### 4.1 Barbería y barberos

- Perfil de cada barbero: nombre, foto, servicios que hace, horario semanal, francos y vacaciones.
- Horario de atención del local.

### 4.2 Servicios

- Catálogo propio de cada barbería, que carga a mano **el dueño** (a confirmar si los barberos también pueden).
- Cada servicio tiene nombre, duración y precio. Las combinaciones son servicios propios, con su
  duración y su precio (ej. *Corte* 30 min, *Corte + barba* 45 min, *Corte + tintura* 2 h).
- **La duración y el precio son del local**, no del barbero.
- Excepción (rara, pero posible): el dueño puede ponerle a un barbero un **precio distinto** para un
  servicio. La duración no cambia.
- La duración define cuánto lugar ocupa el turno en la agenda.

### 4.3 Turnos y agenda

- Agenda del día y de la semana, por barbero y del local completo.
- Disponibilidad calculada con el horario del barbero, la duración del servicio y los turnos ya tomados.
  Un barbero nunca tiene dos turnos superpuestos.
- Se ofrecen horarios cada 15 minutos y también justo cuando termina otro turno (después de una barba de
  20 minutos que empieza a las 9, se puede dar las 9:20). Se dan turnos hasta 90 días para adelante.
- Se elige barbero o "cualquiera"; con "cualquiera", el sistema asigna uno libre en el horario pedido.
- La barbería también carga turnos a mano (alguien que llamó o pasó por el local).
- **Cancelación:** el cliente puede cancelar hasta **24 horas antes**. Avisa por WhatsApp y la barbería
  cancela el turno a mano.
- Reprogramar un turno (otro día u horario y, si lo hace el dueño, otro barbero).
- El barbero carga y mueve solo sus turnos; el dueño, los de todos.
- No se puede dar de baja ni dejar de atender a alguien con turnos reservados; al cargar un día cerrado o
  una ausencia que pisa turnos, el sistema avisa para que se reprogramen.

### 4.4 Atención "tipo Uber"

- Estados del turno: **Reservado → En curso → Finalizado**, o bien **Cancelado** / **Ausente**.
- Al finalizar, el barbero confirma el servicio realizado, el monto y **marca** el medio de pago:
  **efectivo** o **transferencia**. Es solo para llevar el control: el sistema no cobra ni maneja dinero.
- **Modificar un servicio ya hecho:** si se agregó algo (ej. pasó de corte a corte + barba), se corrige
  después de finalizado. El cambio queda en el historial.
- **Atención sin turno:** un apartado para cargar a quien entró sin reservar, para que también cuente en las métricas.
- Historial de cada turno: qué cambió, quién lo cambió y cuándo.

Cómo quedó en la etapa 4:

- **Empezar** se puede el mismo día del turno (el cliente puede llegar antes o tarde).
- **Finalizar** es un toque: el servicio reservado al precio de ese barbero (su precio especial, si tiene) y
  un botón por medio de pago. Para cambiar el servicio o el monto, "Cambiar". Si el barbero se olvidó de
  tocar "Empezar", un turno cuya hora ya pasó se finaliza igual. El horario reservado no cambia.
- **No vino** se marca recién cuando llega la hora del turno, y libera el horario que queda.
- **Deshacer:** un "Empezar" o un "No vino" tocados sin querer vuelven a reservado (si nadie tomó ese
  horario mientras tanto). Una atención sin turno cargada por error se anula.
- **Sin turno:** se elige quién y el servicio, y se empieza ahora; o, si ya se atendió, se carga directo con
  el cobro. El nombre del cliente es opcional. Puede pisarse unos minutos con un turno (el barbero lo mete
  entre dos), pero la agenda no ofrece a otros el rato que ocupa; si termina antes, ese rato se libera.
- **Sin cerrar:** un turno reservado cuya hora ya pasó se marca "Sin cerrar" en la agenda, y la agenda de
  hoy lista los que quedaron abiertos de los últimos 30 días.

### 4.5 Métricas

- **Por barbero (solo las suyas):** cantidad de servicios y por tipo, cobrado total y por medio de pago
  (efectivo / transferencia), por día / semana / mes, ausencias.
- **Del negocio (dueño):** lo mismo para todos los barberos, comparados entre sí.
- No hay comisiones ni propinas.

Cómo quedó en la etapa 5 (pestaña "Números"): de entrada, el mes actual; se cambia a día o semana y se va
para atrás o adelante. Arriba, lo cobrado y cuánto en efectivo y por transferencia; después atenciones (y
cuántas fueron sin turno), los que no vinieron y los cancelados (y cuántos con poco aviso); el cobrado por día,
la comparación entre barberos (el dueño) y lo que se hizo de cada servicio. Avisa los turnos que ya pasaron y
nadie cerró, porque todavía no cuentan. Un turno cuenta en el día en que empezó.

### 4.6 Reserva online con aviso por WhatsApp (gratis)

- Cada barbería tiene su link público (ej. `turnos.chi2.com/barberia-juan`) para compartir en Instagram,
  en los estados o en la respuesta automática de WhatsApp Business.
- El cliente elige servicio, barbero o "cualquiera", día y horario libre, y pone su nombre y teléfono.
  (El servicio hace falta para saber cuánto dura el turno.)
- Al confirmar, se abre WhatsApp en el celular del cliente con un mensaje ya escrito para el número de la
  barbería (ej. *"Hola, reservé Corte con Juan el jueves 21 a las 17:00. Soy Martín."*); el cliente solo
  toca enviar. Es un link `wa.me`, como hacen muchos locales de comida: sin API de WhatsApp y sin costo.
- El turno queda registrado en el sistema al confirmar (ver §9.1).
- **Idea (propuesta):** recordatorio en un toque. Desde la agenda, el barbero toca "Recordar" y se abre
  WhatsApp con el mensaje armado para el cliente. Mismo mecanismo, también gratis.

### 4.7 Celular primero

- El uso principal es desde el **celular**; la computadora es circunstancial.
- En el celular tiene que verse perfecto y ser muy intuitivo: pocas pantallas, acciones en uno o dos toques.
- Es una sola aplicación web, instalable en la pantalla de inicio (PWA), sin pasar por las tiendas de apps.
  La app nativa queda para más adelante; la API REST ya se piensa para eso.
- Usa la misma marca que la web principal (logo, colores y tipografías), para que el salto de una a otra
  se sienta como un solo producto.

## 5. Etapas

Cada etapa se entrega completa y probada antes de pasar a la siguiente (una o varias PRs chicas por etapa).

| # | Etapa | Incluye | Lista cuando… |
|---|---|---|---|
| 0 | **Base del proyecto** | Spring Boot, Postgres, Tailwind, tests, marca de CHI2. | ✅ Hecha. |
| 1 | **Barberías, usuarios y acceso** | Modelo multi-negocio, roles dueño y barbero, login con sesión que se mantiene en el celular, alta y suspensión de barberías por CHI2, equipo del dueño (sumar, dar de baja, reactivar), cambio de contraseña. | ✅ Hecha. Un dueño entra y ve solo su barbería; un barbero ve solo lo suyo. |
| 2 | **Servicios, barberos y horarios** | §4.1 y §4.2: catálogo con duración y precio, ficha de cada barbero (atiende o no, servicios que hace, precio especial), horario del local y propio de cada barbero (hasta dos turnos por día), días cerrados del local y ausencias de cada barbero. La foto del barbero pasa a la etapa 6, que es donde la ve el cliente. | ✅ Hecha. El dueño carga su barbería completa desde el celular. |
| 3 | **Agenda y disponibilidad** | §4.3: agenda del día con la semana arriba (de todos o por barbero), turnos cargados por la barbería, asignación de "cualquiera", cancelar (con cancelación tardía) y reprogramar, e historial de cada turno. | ✅ Hecha. La barbería maneja su agenda en el sistema sin superposiciones. |
| 4 | **Atención tipo Uber** | §4.4: empezar, finalizar con medio de pago, ausente, modificar un servicio hecho, atención sin turno, y aviso de turnos sin cerrar. | ✅ Hecha. Cada turno termina finalizado con su cobro marcado, o cancelado / ausente. |
| 5 | **Métricas** | §4.5. | ✅ Hecha. Cada barbero y el dueño ven sus números del mes. |
| 6 | **Reserva online con aviso por WhatsApp** | §4.6, y la foto de cada barbero para que el cliente elija. | El cliente reserva solo desde el link y a la barbería le llega el WhatsApp. |

**v1 vendible: etapas 1 a 6.**

## 6. Fuera de la v1

- Pagos y señas online (el sistema solo marca cómo se cobró).
- Factura electrónica (ARCA).
- Comisiones y propinas.
- API de WhatsApp (mensajes automáticos sin que nadie toque enviar).
- Cancelación hecha por el propio cliente desde el sistema.
- Varias sucursales por barbería.
- App nativa.

## 7. Decisiones tomadas

- Un solo sistema para muchas barberías (ver `CLAUDE.md` §2).
- Stack igual al de la web principal, y la misma marca.
- Servicios cargados a mano por el dueño, con duración y precio del local; precio distinto por barbero como excepción.
- Medios de pago: efectivo y transferencia, solo marcados para control. Sin comisiones ni propinas.
- Reserva del cliente por link, con aviso por WhatsApp vía `wa.me` (gratis, sin API).
- Cancelación hasta 24 horas antes, cargada a mano por la barbería.
- Se puede modificar un servicio ya finalizado; el cambio queda en el historial.
- Hay un apartado para cargar atenciones sin turno.
- El barbero ve solo lo suyo; el dueño ve todo el negocio.
- Uso principal desde el celular.

## 8. Supuestos (propuestos, a confirmar)

Los de "cualquiera" y la cancelación tardía ya funcionan así desde la etapa 3; si se decide otra cosa, se cambian.

- El cliente no tiene cuenta: se identifica con su teléfono, y cada barbería tiene sus propios clientes.
- Asignación de "cualquiera": entre los barberos libres que hacen ese servicio, el que tiene menos turnos ese día.
- Si un cliente avisa con menos de 24 horas, el turno se puede cancelar igual, pero queda marcado como
  *cancelación tardía* (cuenta aparte en las métricas).
- Una barbería tiene un solo local.

## 9. Preguntas abiertas

1. **Reserva desde el link:** al confirmar, ¿el turno queda **reservado directo** (propuesta: el horario se
   bloquea al instante para que nadie más lo tome; si era falso o el cliente nunca mandó el WhatsApp, la
   barbería lo cancela) o queda **pendiente** hasta que la barbería lo confirme?
2. **Entrada desde la web principal:** el botón "Ingresar" en la web de CHI2 hoy está fuera de su v1
   (ver `docs/01-vision-alcance.md` del repo `chi2`). Coordinar con Nahuel para sumar al menos un link.

## 10. Riesgos

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Los barberos no cierran los turnos y las métricas quedan incompletas. | Alto | Finalizar en dos toques (abrir el turno y tocar el medio de pago); la agenda de hoy avisa los turnos que quedaron sin cerrar (etapa 4). |
| Reservas falsas o de broma desde el link público, que bloquean horarios. | Medio | Pedir nombre y teléfono; límite de reservas abiertas por teléfono; cancelar en un toque. |
| El cliente confirma en el link pero nunca manda el WhatsApp. | Bajo | El turno igual queda en la agenda (§9.1), así que la barbería lo ve. |
| Seis etapas en unos dos meses, en paralelo con la facultad. | Medio | Etapas chicas y en orden; si hace falta, recortar métricas a lo mínimo. |
| Presupuesto: los USD 10 por mes eran para la web; este sistema necesita su propio hosting y base. | Medio | Revisarlo al elegir hosting (ej. una sola base Postgres para los dos); el primer cliente lo paga. |
| Dos turnos para el mismo barbero y horario al mismo tiempo. | Medio | La base de datos impide turnos superpuestos para un mismo barbero. |

## 11. Criterio de "v1 terminada"

- [ ] Una barbería real lo usa: sus clientes reservan con el link, los barberos cierran cada turno marcando el cobro y el dueño ve las métricas del mes.
- [ ] Funciona perfecto en el celular y se puede instalar en la pantalla de inicio.
- [ ] Está publicado con dominio y HTTPS, y se entra desde la web principal de CHI2.
