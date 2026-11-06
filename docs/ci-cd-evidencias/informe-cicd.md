# Informe de laboratorio · CI/CD multinube para App ToDo

**Curso:** Desarrollo y Operaciones de Software (DOSW) · Escuela Colombiana de Ingeniería Julio Garavito
**Docente:** Rodrigo Gualtero
**Integrantes:** [COMPLETAR]
**Repositorio:** Laboratorio8_Thomas_AppToDo_Backend

> **Antes de entregar:** los puntos marcados con **⚠️ COMPLETAR** necesitan datos que solo ustedes tienen (porcentajes de cobertura, hallazgos reales de ESLint/SpotBugs, capturas). Revisen también que lo dicho sobre `ci-quality.yml` coincida con su archivo real, porque su workflow de CI aparece en GitHub como "CI - Backend Calidad y Contenedor" y el frontend se publica desde un repositorio/imagen propios (`...frontend-web`).

## Resumen de lo desplegado

| Elemento | Azure | AWS Academy |
|---|---|---|
| Máquina | VM `vm-dosw-todo-azure` (Ubuntu 24.04, 2 vCPU / 4 GiB, North Central US) | EC2 `ec2-dosw-todo-aws` (`t3.medium`, 2 vCPU / 4 GiB, us-east-1) |
| IP pública | `20.88.52.58` (estática) | `52.45.205.228` (IP elástica) |
| Dominio | `20-88-52-58.sslip.io` | `52-45-205-228.sslip.io` |
| Runner / etiqueta | `azure-todo` | `aws-todo` |
| Environment de GitHub | `azure-lab` | `aws-lab` |
| Workflow | `deploy-azure.yml` | `deploy-aws.yml` |
| Directorio de despliegue | `~/todo-azure` | `~/todo-aws` |

Imágenes desplegadas (GHCR, ambas nubes): `laboratorio8_thomas_apptodo_backend-api` con tag `sha-<commit de main>` y `laboratorio8_thomas_apptodo_frontend-web` con un tag `sha-6c1035e…` fijado en el workflow.

---

## 1. ¿Diferencia entre integración continua, entrega continua y despliegue continuo? ¿Cuál implementamos?

- **Integración continua (CI):** cada cambio se compila, se prueba y se analiza automáticamente para detectar errores pronto.
- **Entrega continua (continuous delivery):** el software siempre queda en un estado desplegable (por ejemplo, con artefactos versionados), pero el paso a producción lo decide una persona.
- **Despliegue continuo (continuous deployment):** todo cambio que pasa las pruebas llega a producción automáticamente, sin intervención humana.

Implementamos **CI completa y entrega continua**. El workflow de CI corre solo con cada push y pull request, y en `main` publica imágenes versionadas por SHA en GHCR. El despliegue en Azure y AWS usa `workflow_dispatch`: lo lanza una persona manualmente, así que **no es despliegue continuo**. Esa decisión es deliberada: cada equipo controla cuándo consume sus créditos de nube.

## 2. ¿Por qué `publish-images` debe depender de las pruebas y del análisis estático?

Porque una imagen publicada es la que luego se despliega. Si se publicara sin esperar la calidad, podría llegar a producción código con pruebas rotas, cobertura por debajo del 80 % o defectos detectados por SpotBugs. Con `needs` sobre los jobs de calidad y la condición `push` a `main`, **solo existen en GHCR imágenes cuyo commit pasó todas las verificaciones**. Además, el CD vuelve a ejecutar la calidad (`quality-gate`) antes de desplegar, por lo que una versión no puede desplegarse si hoy fallaría.

Lo comprobamos en la práctica: el CD descarga la imagen con el tag del commit actual, y esa imagen solo existe cuando el CI de ese commit terminó bien.

## 3. ¿Qué verifica JaCoCo y qué NO permite concluir una cobertura del 100 %?

JaCoCo mide qué porcentaje de líneas (y otros contadores) del código se ejecutó durante las pruebas. Nuestro `pom.xml` define una regla `check` sobre el contador `LINE` con mínimo 0,80; si no se cumple, `mvn verify` falla y el pipeline se detiene.

Una cobertura del 100 % **no** permite concluir que el software es correcto: solo dice que las líneas se ejecutaron, no que las pruebas verifiquen resultados correctos (pueden no tener aserciones útiles), ni que se probaran todos los casos límite, combinaciones de datos, concurrencia o integraciones reales.

- Cobertura backend obtenida: **⚠️ COMPLETAR** (valor del reporte JaCoCo)
- Cobertura frontend obtenida: **⚠️ COMPLETAR** (valor del reporte Vitest)
- Exclusiones aplicadas y su justificación: **⚠️ COMPLETAR**

## 4. ¿Cómo intervienen ESLint y SpotBugs? Hallazgos reales

- **SpotBugs** analiza el bytecode Java en busca de patrones de defecto (posibles `NullPointerException`, exposición de estado interno, malas prácticas). Está en la fase `verify` con `check`, así que los hallazgos sobre el umbral configurado rompen el build. Usamos `spotbugs-exclude.xml` para excluir lo justificado.
- **ESLint** analiza el código JavaScript/React sin ejecutarlo (variables sin usar, reglas de hooks, errores de sintaxis o estilo) y se ejecuta con `npm run lint` antes de las pruebas.

Un error estático no es necesariamente un fallo de ejecución: señala código sospechoso que puede funcionar hoy y fallar en otro contexto.

- Hallazgo real de SpotBugs: **⚠️ COMPLETAR** (regla, clase, cómo se corrigió o por qué se excluyó). Si no hubo ninguno, indíquenlo y justifíquenlo.
- Hallazgo real de ESLint: **⚠️ COMPLETAR**.

## 5. ¿Por qué dos Dockerfiles multietapa y qué queda en cada imagen final?

La multietapa separa **construir** de **ejecutar**. Las herramientas pesadas (Maven, Node, el código fuente, las dependencias de desarrollo) se quedan en la etapa de build y no viajan a la imagen final. Resultado: imágenes más pequeñas, con menos superficie de ataque y construcciones reproducibles.

- **Backend:** etapa 1 con `maven:3.9-eclipse-temurin-17` que compila y genera el `.jar`. Etapa 2 con `eclipse-temurin:17-jre`. La imagen final contiene solo el JRE y `app.jar` (sin Maven ni fuentes).
- **Frontend:** etapa 1 con `node:22-alpine` que ejecuta `npm ci` y `npm run build`. Etapa 2 con `nginx:alpine`. La imagen final contiene Nginx, `nginx.conf` y los archivos estáticos de `dist/` (sin Node ni `node_modules`).

## 6. ¿Diferencia entre imagen y contenedor? ¿Qué contiene `todo_pgdata`?

Una **imagen** es una plantilla de solo lectura, inmutable, con el sistema de archivos y la configuración de arranque. Un **contenedor** es una instancia en ejecución de una imagen, con una capa de escritura efímera: si se elimina el contenedor, esa capa se pierde.

`todo_pgdata` es un **volumen Docker** montado en `/var/lib/postgresql/data` del contenedor `db`. Contiene los archivos de datos de PostgreSQL (tablas de usuarios y tareas, índices, etc.). Como vive fuera del contenedor, los datos sobreviven a recrear contenedores; solo se perderían con `docker compose down -v` o borrando el volumen, que por eso nunca usamos en el CD.

## 7. ¿Qué identifica el tag `sha-COMMIT` y por qué es mejor que `latest`?

Identifica de forma única el **commit exacto** del que salió la imagen. Eso da trazabilidad (se sabe qué código corre en cada nube), reproducibilidad (el mismo tag da siempre la misma imagen) y permite hacer **rollback** a una versión concreta cambiando un tag. Con `latest`, el contenido cambia con cada publicación, no se sabe qué versión se desplegó y dos nubes podrían terminar con versiones distintas sin darse cuenta.

En nuestro caso, las dos nubes descargan las mismas imágenes por SHA: no se reconstruyen ni se modifican en cada VM.

## 8. ¿Por qué React usa `/api/...` con proxy y no `localhost:8080`?

El código de React se ejecuta en el **navegador del usuario**, no en el servidor. Un `localhost:8080` allí apuntaría a la máquina del propio usuario, donde no hay backend. Además, el backend (8080) y la base de datos no deben exponerse a Internet.

Con rutas relativas, el navegador llama al mismo origen (`https://<dominio>/api/...`), Caddy entrega la petición a Nginx y Nginx la reenvía a `backend:8080` por la red interna de Docker. Así solo se publican los puertos 80 y 443, y se evita configurar CORS entre dominios distintos.

En la práctica descubrimos un matiz: aun con el proxy, el navegador envía la cabecera `Origin: https://<dominio>` y Spring, que veía la petición como `http` interna, la trató como cruzada y la rechazó con 403 porque `WebConfig` solo permitía `http://localhost:5173`. Lo corregimos leyendo los orígenes permitidos de la variable `CORS_ALLOWED_ORIGINS` (ver pregunta 15).

## 9. ¿Cómo protegimos los secretos? ¿Qué implica dar acceso Docker al runner?

**Protección de secretos:**
- Contraseña de PostgreSQL y secreto JWT se guardan como **secrets del environment** (`azure-lab`, `aws-lab`), distintos para cada nube, y nunca en el repositorio. El `.env` de ejemplo solo trae valores de relleno y `.env` está en `.gitignore`.
- En CI se usan credenciales de prueba que no corresponden a ningún ambiente desplegado.
- El workflow valida con `${VAR:?}` que existan, escribe el `.env` en la VM con `umask 077` (solo legible por su dueño) y hace `docker logout` de GHCR al terminar.
- La autenticación con GHCR usa el `GITHUB_TOKEN` del workflow, sin tokens personales.
- No se guardan llaves SSH ni credenciales de nube en GitHub: el despliegue ocurre dentro de la VM con un runner self-hosted que se conecta hacia afuera por HTTPS.
- Cuidado adicional: no mostrar secretos en capturas ni en logs.

**Implicaciones del acceso Docker al runner:** pertenecer al grupo `docker` equivale a tener permisos de administrador sobre la máquina (se puede montar el disco del host en un contenedor). Por eso el runner se usa solo en un repositorio privado, con código de `main`, en máquinas de laboratorio sin datos reales, y el job de despliegue solo corre si `github.ref == 'refs/heads/main'`. Mejora pendiente: en la configuración de los environments, "Deployment branches" aparece sin restricción; conviene limitarlo a `main`.

## 10. ¿Qué cambió entre Azure y AWS? ¿Qué NO cambió?

**Cambió:** la nube y la máquina (VM vs EC2), el runner y su etiqueta (`azure-todo` / `aws-todo`), el environment y sus secretos y variable de dominio, el directorio de despliegue, y el dominio (basado en cada IP). También cambiaron detalles operativos: en Azure la IP pública fue estática desde el inicio, mientras que en AWS hubo que asignar una **IP elástica** para que no cambiara al detener e iniciar la instancia; y en la EC2 hubo que instalar Docker y reiniciar el runner, porque el job falló con `docker: command not found`.

**No cambió:** las imágenes (se descargan de GHCR por SHA, no se reconstruyen), el `docker-compose.prod.yml`, el `Caddyfile`, el código del backend y frontend, la lógica del workflow y el smoke test (HTTPS, `/health` y 401 sin token).

## 11. Pipeline verde pero la aplicación falla al atender solicitudes

Nos pasó. El CD de Azure terminó en verde (`/health` respondía 200 y `/api/v1/tasks` daba 401 sin token), pero al registrarse desde el navegador la app mostraba "No tienes permisos". La causa era el CORS (pregunta 15): el smoke test solo hacía peticiones GET desde el propio servidor, sin cabecera `Origin`, así que no ejercitaba el camino real del navegador.

Controles adicionales que incluiríamos:
- Un smoke test de extremo a extremo con `curl`: registrar un usuario de prueba, iniciar sesión, crear y leer una tarea con el JWT, incluyendo la cabecera `Origin` del dominio desplegado.
- Un healthcheck del backend en Compose (por ejemplo, el endpoint de salud de Actuator) y espera por él.
- Reversión automática al tag anterior si el smoke test falla.
- Monitoreo y alertas sobre errores 4xx/5xx.

## 12. ¿Por qué las tareas de Azure no aparecen en AWS?

Porque son **dos despliegues completamente independientes**. Cada nube corre su propio contenedor PostgreSQL con su propio volumen `todo_pgdata` en el disco de su máquina, y no existe replicación ni base compartida. Un usuario registrado o una tarea creada en `20-88-52-58.sslip.io` solo existe en la base de datos de Azure, y lo mismo en AWS. Comparten el mismo código y las mismas imágenes, pero no los datos.

- Tarea creada en Azure: **⚠️ COMPLETAR** (título)
- Tarea creada en AWS: **⚠️ COMPLETAR** (título)

## 13. Nueva imagen con fallo cuando el código ya modificó el esquema

Revertir la imagen **no revierte la base de datos**. Como el backend corre con `ddl-auto=validate` y el esquema se crea con scripts SQL solo cuando el volumen está vacío, un cambio incompatible puede dejar la versión anterior sin poder arrancar o funcionando mal.

Lo que haríamos:
1. Detener el daño: volver a la imagen anterior si el esquema es compatible hacia atrás.
2. Si no lo es, aplicar un **script de migración inverso** (o restaurar un respaldo de la base, que deberíamos tomar antes de desplegar cambios de esquema, por ejemplo con `pg_dump`).
3. A futuro, usar migraciones versionadas (Flyway o Liquibase) y diseñar los cambios de esquema en dos pasos compatibles (agregar primero, eliminar después) para que el rollback de imagen sea seguro.

## 14. Recursos que siguen generando costo tras detener una VM

- **Discos:** el disco administrado de Azure y los volúmenes EBS de AWS siguen cobrándose aunque la máquina esté detenida.
- **Direcciones IP públicas fijas:** la IP estática de Azure y la IP elástica de AWS (AWS cobra por IPv4 pública) siguen costando.
- **Snapshots, imágenes y otros recursos asociados** (interfaces de red, grupos de seguridad no tienen costo, pero sí los recursos de pago que dependen de ellos).
- En Azure, apagar desde Linux (`shutdown`) **no desasigna** la VM y sigue cobrando cómputo; hay que usar **Detener** en el portal.
- En AWS Academy, el laboratorio se detiene con *Stop Lab*, pero los recursos pueden tener reglas especiales de persistencia.

Recursos que dejamos activos y su motivo: **⚠️ COMPLETAR**. Saldo de créditos al terminar: **⚠️ COMPLETAR** (captura de Cost Management / Billing).

## 15. Dificultades técnicas reales y cómo se diagnosticaron

**a) Dominio inexistente: HTTPS y DNS (Azure).**
- *Síntoma:* el paso de verificación del CD fallaba con `curl: (6) Could not resolve host: todo.apptodo.edu.co`.
- *Diagnóstico:* desde la VM, `curl -i http://localhost/health -H "Host: ..."` respondía `308` hacia `https://todo.apptodo.edu.co/health` (Caddy funcionaba), pero `curl -k https://127.0.0.1/health` fallaba con `tlsv1 alert internal error`: Caddy elige el certificado por SNI y no tenía uno para ese nombre. Además, el dominio no existía en ningún DNS, así que Let's Encrypt no podía validarlo. También había un bug en el script: el bucle nunca asignaba `ok=1`.
- *Solución:* usar `20-88-52-58.sslip.io` como `AZURE_DOMAIN` (resuelve a la IP de la VM), corregir el script (sin `-k` ni `--resolve`, con `ok=1`) y abrir 80/443. En los logs de Caddy apareció `authorization finalized` y `validations succeeded`, y `curl -I https://20-88-52-58.sslip.io/health` devolvió `HTTP/2 200`.

**b) CORS rechazaba el registro desde el navegador (403).**
- *Síntoma:* "No tienes permisos para realizar esta acción" al registrarse; la consola mostraba 403 en `/api/v1/auth/register`.
- *Diagnóstico:* con PowerShell, el POST sin cabecera `Origin` devolvía 400 (la seguridad lo dejaba pasar) y con `Origin: https://20-88-52-58.sslip.io` devolvía 403. Los logs del backend estaban en silencio, típico de un rechazo CORS. `SecurityConfig` estaba bien (CSRF deshabilitado, registro y login con `permitAll`); el problema estaba en `WebConfig`, que solo permitía `http://localhost:5173`.
- *Solución:* leer los orígenes de `CORS_ALLOWED_ORIGINS` en `WebConfig` y definirla en el compose como `https://${TODO_DOMAIN}`. Así el mismo código sirve para local, Azure y AWS.

**c) Docker no instalado en la EC2 (AWS).**
- *Síntoma:* el job fallaba con `docker: command not found` y `exit code 127`.
- *Diagnóstico:* el runner sí estaba Online y el checkout funcionaba; faltaba Docker en la instancia.
- *Solución:* instalar Docker y Compose (comandos del apartado 11), cerrar y reabrir SSH para que aplicara el grupo `docker` (antes salía `permission denied` sobre `docker.sock`), reiniciar el servicio del runner y volver a ejecutar el workflow, que terminó en verde.

---

## Evidencias

Las evidencias están en `docs/ci-cd-evidencias/`. **⚠️ COMPLETAR** con los nombres de archivo reales: tests y cobertura, análisis estático, pipeline rojo y verde, imágenes en GHCR, runners, pipelines de Azure y AWS, URLs con CRUD, 401 sin JWT, persistencia, y apagado y revisión de consumo.

**Video final:** [COMPLETAR enlace]
