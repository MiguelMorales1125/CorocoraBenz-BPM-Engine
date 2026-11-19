# CB-BPM-Engine (`cb-bpm-engine`)

Motor de procesos Camunda 7.24 embebido en Spring Boot 3.5.6 (Java 17): despliega y ejecuta los tres procesos de negocio de Corocora Benz y orquesta a los demás sistemas.

> **Este repositorio es un componente de Corocora Benz. El hub integrador, con la guía general, está en https://github.com/MiguelMorales1125/Corocora-Benz.**

Repositorio de este componente: https://github.com/MiguelMorales1125/CorocoraBenz-BPM-Engine

## Propósito

Es el único sistema que conoce los procesos. Los otros cinco (Portal Cliente, Portal TI, CentralSys, Banco y Taller-Bodega) solo inician procesos, atienden tareas o responden a sus llamadas. Los modelos están descritos en [PROCESOS.md](https://github.com/MiguelMorales1125/Corocora-Benz/blob/main/docs/PROCESOS.md).

| Proceso principal | Carriles | Subproceso reutilizable (call activity) | Decisión DMN |
|---|---|---|---|
| `CB_GestionIncidenteTI` | Usuario Interno Solicitante, Mesa de Servicio, Responsable del Servicio TI | `CB_ResolverIncidenteTI` (Soporte TI Nivel 1, Soporte Especializado) | `CB_ClasificarIncidenteTI` |
| `CB_GestionNomina` | Analista de Nómina, Gestión de Talento Humano, Jefatura de Nómina, Tesorería | `CB_ValidarNovedadEmpleado` (Analista de Nómina, Gestión de Talento Humano) | `CB_ClasificarExcepcionNomina` |
| `CB_PostventaGarantias` | Asesor de Servicio, Analista de Garantías, Coordinador de Postventa | `CB_AtenderOrdenTaller` (Técnico de Taller, Inspector de Calidad) | `CB_EvaluarCoberturaGarantia` |

**Despliegue automático.** `Application` lleva `@EnableProcessApplication` y `META-INF/processes.xml` activa el escaneo del classpath con los sufijos `form, dmn, bpmn`. Al arrancar, el motor despliega por sí solo los 6 BPMN de `src/main/resources/static.bpmn/` (`principales/` y `reutilizables/`), las 3 tablas de `static.dmn/` y los 22 formularios de `static.form/`. Esas carpetas son copia idéntica de `bpmn/`, `dmn/` y `form/` del hub: un cambio en un modelo se hace en los dos sitios. En Tasklist solo se pueden iniciar los tres procesos principales.

**Cómo se comunica con los demás sistemas** (toda llamada lleva la credencial interna `X-CB-Internal`; detalle en el [contrato de integración](https://github.com/MiguelMorales1125/Corocora-Benz/blob/main/docs/CONTRATO-INTEGRACION.md)):

1. *Entrada por la REST de Camunda* (`/engine-rest/*`): los portales validan usuarios (`/identity/verify`, `/group?member=`), inician procesos (`/process-definition/key/{clave}/start`) y consultan o completan tareas (`/task/...`).
2. *Mensajes de correlación* (`POST /engine-rest/message`, siempre por `businessKey`):

| Mensaje | Lo envía | Efecto en el proceso |
|---|---|---|
| `SolicitudServicio` | Portal Cliente | Inicia `CB_PostventaGarantias` |
| `CotizacionAceptada`, `CotizacionRechazada` | Portal Cliente | Respuesta del cliente a la cotización |
| `RepuestosDisponibles` | Taller-Bodega | Libera la espera `waitParts` de Postventa |
| `RepuestoRecibido` | Taller-Bodega | Libera la espera `receivePart` de `CB_AtenderOrdenTaller` |
| `DispersionAceptada`, `DispersionRechazada` | Banco | Respuesta del banco a la dispersión de nómina |

3. *Conectores HTTP* (`http-connector`) que el motor invoca desde tareas de servicio:

| Tarea BPMN | Llamada |
|---|---|
| `createTicket`, `recordResolution`, `closeTicket` / `closeAutoTicket` | Portal TI: `POST /api/tickets`, `POST /api/tickets/{ticketId}/solucion`, `POST /api/tickets/{ticketId}/cerrar` |
| `sendBankBatch` | Banco: `POST /api/dispersiones` |
| `vehicleLookup`, `prepareQuote`, `reserveParts`, `requestPart` | Taller-Bodega: `GET /api/vehiculos/{vin}?km=`, `POST /api/cotizaciones`, `POST /api/reservas`, `POST /api/solicitudes-repuesto` |

   Los BPMN usan dos beans: `sistemas` (URL de cada sistema y `${sistemas.token}`) y `json` (arma el cuerpo con la `businessKey` y lee la respuesta; una respuesta 4xx/5xx lanza una excepción, el job se reintenta y, si sigue fallando, queda un incidente visible en Cockpit).
4. *JavaDelegates* (`delegateExpression`): `obtenerNovedadesDelegate` (`fetchChanges`), `calcularLiquidacionDelegate` (`calculatePayroll`), `generarComprobantesDelegate` (`createReceipts`) y `registrarPagoDelegate` (`recordPayment`) en Nómina, con datos simulados; `enviarNotificacionDelegate` (`notifySolution`, `notifyAutoClose`, `notifyRejection`) envía los correos o, por defecto, los deja en el log.

Además, `ClaveDeNegocioAutomatica` da una `businessKey` (`TI-`, `NOM-` o `PV-` más 8 caracteres) a toda instancia raíz que arranque sin ella, por ejemplo desde Tasklist.

## Puerto y API

Puerto **9000**.

| Método y ruta | Quién la llama | Qué hace |
|---|---|---|
| `/engine-rest/*` | Portales, Banco y Taller-Bodega (con credencial interna) | REST estándar de Camunda |
| `POST /api/demo/processes/postventa`, `/ti`, `/nomina` | Swagger UI, pruebas manuales | Inicia un proceso con `{businessKey, variables}` |
| `GET /api/demo/tasks?processInstanceId=` | Swagger UI, pruebas manuales | Lista las tareas humanas activas |
| `POST /api/demo/tasks/{taskId}/complete` | Swagger UI, pruebas manuales | Completa una tarea con `{variables}` |
| `POST /api/demo/events/{evento}/{businessKey}` | Swagger UI, pruebas manuales | Correlaciona un mensaje; `evento` es `cotizacion-aceptada`, `cotizacion-rechazada`, `repuesto-recibido`, `dispersion-aceptada` o `dispersion-rechazada` |
| `GET /swagger-ui.html`, `GET /v3/api-docs` | Navegador (público) | Documentación de `/api/demo`; el botón "Authorize" fija la credencial interna |

`/engine-rest/*` y `/api/demo/*` responden 401 sin la credencial interna. Las webapps de Camunda tienen su propio inicio de sesión (`demo` / `demo`): Cockpit en http://localhost:9000/camunda/app/cockpit/ y Tasklist en http://localhost:9000/camunda/app/tasklist/.

**Cuentas de demostración.** La cuenta `demo` (grupo `camunda-admin`) la crea Camunda (`camunda.bpm.admin-user`); las demás las crea `IdentidadesDemo` al arrancar, en cualquier perfil, con correo `<id>@corocorabenz.test`. Contraseña `demo`, salvo `USR-01`, cuya contraseña es `USR-01`. Los portales validan el inicio de sesión contra estas cuentas.

| Cuentas | Grupos (en el mismo orden) |
|---|---|
| `mesa1`, `soporte1`, `especialista1`, `responsable1` | `mesa-servicio`, `soporte-ti-n1`, `soporte-ti-especializado`, `responsables-ti` |
| `nomina1`, `talento1`, `jefatura1`, `tesoreria1` | `analistas-nomina`, `gestion-talento-humano`, `jefatura-nomina`, `tesoreria` |
| `asesor1`, `garantia1`, `coordinador1` | `asesores-servicio`, `analistas-garantia`, `coordinadores-postventa` |
| `tecnico1`, `calidad1`, `bodega1` | `tecnicos-taller`, `inspectores-calidad`, `bodega` |
| `cliente1`, `cliente2` | `clientes` |
| `USR-01` | (empleado sin grupo; reporta incidentes) |

## Datos

Usa la base `corocora_camunda`, solo con las tablas del motor (`ACT_*`), que Camunda crea y actualiza al arrancar (`schema-update: true`, historial `full`). No tiene tablas de negocio: esas viven en la base `corocora` de los otros sistemas ([modelo de datos](https://github.com/MiguelMorales1125/Corocora-Benz/blob/main/docs/MODELO-DE-DATOS.md)).

## Configuración

| Variable de entorno | Para qué | Valor por defecto |
|---|---|---|
| `SERVER_ADDRESS` | Interfaz en la que escucha | `127.0.0.1` |
| `DB_HOST`, `DB_PORT`, `DB_ENGINE_NAME` | Servidor PostgreSQL y base del motor | `localhost`, `5432`, `corocora_camunda` |
| `DB_USER`, `DB_PASSWORD` | Usuario y contraseña de PostgreSQL | `postgres` y la contraseña local definida en `application.yaml` |
| `COROCORA_INTERNAL_TOKEN` | Credencial interna; debe ser la misma en los seis sistemas | `cb-local-interno-demo` |
| `COROCORA_DEMO_IDENTIDADES` | `false` para no crear las cuentas de demostración | `true` |
| `COROCORA_DEMO_PASSWORD` | Contraseña de las cuentas que se creen (no cambia las existentes) | `demo` |
| `PORTAL_TI_URL`, `BANCO_URL`, `TALLER_BODEGA_URL` | Destino de los conectores HTTP | `http://localhost:9002`, `http://localhost:9004`, `http://localhost:9005` |
| `CORREO_HABILITADO` | `true` envía las notificaciones por SMTP; `false` las deja en el log | `false` |
| `MAIL_HOST`, `MAIL_PORT` | Servidor SMTP | `localhost`, `1025` |

Perfil `h2` (`--spring.profiles.active=h2`): reemplaza PostgreSQL por una base H2 en memoria (`jdbc:h2:mem:corocora_camunda`) que se borra al detener el motor.

## Compilar y ejecutar

Este repositorio no compila clonado por separado: su `pom.xml` hereda del pom padre del hub (`com.corocorabenz:corocora-benz:1.0.0-SNAPSHOT`) y usa la librería `cb-comun`. Se compila desde el hub, con Java 17 o superior (Maven no hace falta: el hub trae su wrapper).

```powershell
git clone https://github.com/MiguelMorales1125/Corocora-Benz.git
cd Corocora-Benz/corocora-benz-embebida
.\mvnw.cmd -pl cb-bpm-engine -am package -DskipTests
java -jar cb-bpm-engine/target/cb-bpm-engine-1.0.0-SNAPSHOT.jar --spring.profiles.active=h2
```

En Linux o macOS se usa `./mvnw` en lugar de `.\mvnw.cmd`; lo demás es igual. Sin `--spring.profiles.active=h2` usa PostgreSQL y necesita la base `corocora_camunda` (se crea con `psql -U postgres -f crear-bases.sql`, script del hub).

- **Orden de arranque:** el motor va primero; los demás sistemas validan contra él los usuarios. Para que los conectores HTTP funcionen deben estar arriba Portal TI (9002), Banco (9004) y Taller-Bodega (9005).
- **Comprobar que arrancó:** http://localhost:9000/swagger-ui.html debe abrir, y `curl -H "X-CB-Internal: cb-local-interno-demo" http://localhost:9000/api/demo/tasks` debe responder 200 con una lista JSON (en PowerShell, `curl.exe`).

## Pruebas

```powershell
.\mvnw.cmd -pl cb-bpm-engine -am test
```

Son 42 pruebas con H2, sin ningún otro sistema arrancado (`-am` ejecuta además las de `cb-comun`):

- `ModelosDesplegadosTest` (8): los modelos desplegados, sus formularios de inicio y de tarea, y el vencimiento de la espera del banco.
- `ClaveDeNegocioAutomaticaTest` (9): `businessKey` automática por proceso y su herencia en los subprocesos.
- `IdentidadesDemoTest` (10) e `IdentidadesDemoConfiguracionTest` (3): cuentas, grupos, correos y contraseñas de demostración, y su desactivación.
- `RutasInternasTest` (9): la credencial interna en `/api/demo` y `/engine-rest`, errores en JSON y OpenAPI público.
- `ConectoresConCredencialTest` (3): todos los conectores HTTP de los BPMN envían `X-CB-Internal`.

Los escenarios de punta a punta están en [CASOS-DE-PRUEBA.md](https://github.com/MiguelMorales1125/Corocora-Benz/blob/main/docs/CASOS-DE-PRUEBA.md).

## Estructura del código

Paquete base `com.corocorabenz.engine` (`src/main/java`):

- `Application`, `OpenApiConfiguration`: arranque como aplicación de procesos y configuración de Swagger.
- `connector`: beans `sistemas` y `json` que usan los conectores HTTP de los BPMN.
- `delegate.nomina`, `delegate.comun`: JavaDelegates de Nómina y de notificaciones.
- `notificacion`: `CorreoService`, envío por SMTP o registro en el log.
- `identidad`: `IdentidadesDemo`, cuentas y grupos de demostración.
- `proceso`: `ClaveDeNegocioAutomatica`.
- `controller`: API de demostración `/api/demo` y sus ejemplos de Swagger.
- `src/main/resources`: `application.yaml`, `META-INF/processes.xml`, `static.bpmn/`, `static.dmn/`, `static.form/`.
