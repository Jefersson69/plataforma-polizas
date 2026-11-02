# polizas-api

Spring Boot 4.1, Java 21 y H2 en memoria. En `local` usa la clave `123456` que pide la prueba y carga los datos de ejemplo de `datos-demo.json`, sin necesitar nada más.

```bash
./mvnw spring-boot:run
./mvnw test
```

## Endpoints

Todos piden el header `x-api-key`.

| Ruta | Qué hace |
|---|---|
| `GET /polizas?tipo=&estado=` | Lista pólizas |
| `GET /polizas/{id}/riesgos` | Lista los riesgos de una póliza |
| `POST /polizas/{id}/renovar` | Aplica el IPC y la deja en `RENOVADA` |
| `POST /polizas/{id}/cancelar` | Cancela la póliza y sus riesgos |
| `POST /polizas/{id}/riesgos` | Agrega un riesgo, solo en colectivas |
| `POST /riesgos/{id}/cancelar` | Cancela un riesgo |
| `POST /core-mock/evento` | Mock del CORE, registra el intento en logs |
| `GET /ipc` y `PUT /ipc/{anio}` | Consulta o carga manual del IPC |

## Lo que decidí

- Solo se puede renovar desde 60 días antes del vencimiento, para que un doble clic no aplique el IPC dos veces. Es configurable y falta confirmarlo con negocio.
- El IPC sale de una tabla que se sincroniza con el Excel del DANE, porque no encontré una API oficial. Si falta el dato responde `503`, no se inventa un valor.
- Al CORE se le avisa después del commit.

## Lo que falta

Si el CORE falla el aviso no se reintenta, y la conexión queda ocupada mientras se le avisa. Eso lo resolvería con un outbox. Tampoco hay paginación ni migraciones.
