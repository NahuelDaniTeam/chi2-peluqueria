# chi2-peluqueria

Sistema de turnos de CHI2 para barberías y peluquerías. Un solo sistema, muchas barberías.

Requisitos: JDK 21 y Docker Desktop.

```bash
./mvnw spring-boot:run   # http://localhost:8080
./mvnw test
```

Para tener el primer usuario (CHI2), definí `CHI2_ADMIN_EMAIL` y `CHI2_ADMIN_CONTRASENA` antes de
arrancar (ver `CLAUDE.md` §5).

Contexto, decisiones y reglas de trabajo: ver [`CLAUDE.md`](CLAUDE.md).
