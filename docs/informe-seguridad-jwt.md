# Respuesta a las preguntas del laboratorio, informe del laboratorio.

## 1. ¿Cuál es la diferencia entre autenticación y autorización? Dé un ejemplo real de ToDo.

* **Autenticación (¿Quién eres?):** Es el proceso mediante el cual el sistema verifica la identidad de un usuario a través de credenciales (correo y contraseña). En **AppToDo**, esto ocurre cuando un usuario envía sus datos mediante el endpoint `POST /api/v1/auth/login`. Si el sistema valida la contraseña mediante el hash de BCrypt, emite un token JWT firmado.
* **Autorización (¿Qué tienes permiso de hacer?):** Es el proceso posterior que determina si un usuario autenticado cuenta con los privilegios necesarios para ejecutar una acción o acceder a un recurso específico.

> **Ejemplo real en AppToDo:**
> * **Autenticación:** Ana introduce su correo y contraseña correctos en la interfaz de login de React, obteniendo su JWT.
> * **Autorización:** Cuando Ana intenta acceder al endpoint administrativo `GET /api/v1/admin/summary`, Spring Security verifica los *claims* de su JWT. Como su rol es `USER`, el servidor rechaza la petición con código **`403 Forbidden`**. En cambio, si Luis, que posee el rol `ADMIN`, realiza la misma petición, el servidor lo autoriza y responde con éxito (**`200 OK`**).

## 2. Explique cómo se genera un JWT, qué contienen sus partes y cómo verifica Spring Boot que no fue manipulado.

En la implementación de `JwtServiceImpl.java`, el token se genera utilizando la librería Nimbus JOSE + JWT (`JwtEncoder`) con el algoritmo simétrico **HMAC-SHA256 (HS256)**. Un JWT consta de tres partes separadas por puntos (`.`):
1. **Header (Cabecera):** Especifica el tipo de token y el algoritmo de firma criptográfica utilizado (`alg: HS256`).
2. **Payload (Carga útil):** Contiene los *claims* o declaraciones del usuario, configurados en el código mediante el `JwtClaimsSet`:
   * `iss` (Emisor): `todo-api`
   * `sub` (Sujeto): El identificador único del usuario (ej. `"1"`).
   * `iat` (Fecha de emisión): Marca temporal de creación.
   * `exp` (Fecha de expiración): Fijada en 1800 segundos (30 minutos).
   * `role`: El rol del usuario (`USER` o `ADMIN`).
3. **Signature (Firma):** Se calcula tomando el Header y el Payload codificados en Base64Url, combinándolos con la clave secreta del servidor (`JWT_SECRET`) mediante el algoritmo HS256.

**Cómo verifica Spring Boot que no fue manipulado:**
A través de `JwtConfig.java`, el componente `JwtDecoder` utiliza la misma clave secreta (`SecretKey`) con la que se firmó el token. Cuando una petición llega con una cabecera `Authorization: Bearer <token>`, Spring Security recalcula matemáticamente la firma usando el Header y el Payload recibidos junto con la clave secreta guardada en el servidor. Si un atacante modifica un solo carácter del payload, la firma recalculada no coincidirá con la firma adjunta en el token, por lo que el decodificador la invalidará automáticamente y rechazará la petición con `401 Unauthorized`.

## 3. ¿Por qué decodificar el payload de un JWT no equivale a validar su autenticidad?

El Payload de un JWT está codificado únicamente en **Base64URL**, un formato de codificación de texto y no de cifrado. Cualquier persona (incluido un atacante) puede interceptar el token, copiar el payload y decodificarlo instantáneamente en texto plano para leer los datos que contiene (como el ID o el rol).

Sin embargo, **decodificar no es validar**. Decodificar solo permite leer la información visualmente, pero no comprueba si el token fue emitido por un servidor legítimo ni si sus datos fueron alterados en tránsito. Para validar la **autenticidad**, es estrictamente obligatorio verificar la **firma criptográfica** y la **marca de tiempo de expiración** en el backend. Si un atacante altera el payload y vuelve a codificarlo en Base64 sin conocer la clave secreta (`JWT_SECRET`), el backend detectará que la firma no coincide y rechazará el token, demostrando que la simple decodificación carece de seguridad.

## 4. ¿Qué sucede exactamente cuando un token vence y el usuario intenta consultar sus tareas?

1. **Rechazo en el Backend:** El token JWT incluye una fecha de expiración (`exp`) configurada para 30 minutos. Al intentar consultar `GET /api/v1/tasks`, el `JwtTimestampValidator` en Spring Boot detecta que la marca de tiempo actual supera el límite de expiración. El servidor rechaza la solicitud respondiendo con el código HTTP **`401 Unauthorized`**.
2. **Intercepción en el Frontend:** En el cliente HTTP (`http.js`), se evalúa la respuesta: `if (response.status === 401 && auth)`. Al cumplirse la condición, se ejecuta la función `notifyUnauthorized()`.
3. **Limpieza y Redirección:** El `AuthProvider` captura el evento, ejecuta `clearSession()` (purgando el token de la memoria RAM) y limpia el estado del usuario (`setUser(null)`). Esto obliga a la interfaz a mostrar un aviso de sesión expirada (*"Tu sesión expiró. Inicia sesión de nuevo."*) y a retornar al usuario al formulario de inicio de sesión.

## 5. ¿Por qué no se deben almacenar las contraseñas en texto plano? ¿Cómo verifican la contraseña durante el login?

* **Por qué no en texto plano:** Si una base de datos es vulnerada o comprometida, almacenar contraseñas en texto plano expondría inmediatamente las cuentas de todos los usuarios, permitiendo que atacantes reutilicen esas credenciales en otros servicios (ataques de credenciales cruzadas).
* **Cómo se verifican durante el login (`AuthServiceImpl.java`):**
  1. Al registrarse, la contraseña se procesa mediante un hash unidireccional utilizando **BCrypt** (`passwordEncoder.encode(request.getPassword())`), el cual añade un *salt* único y un factor de trabajo elevado.
  2. Al iniciar sesión (`login`), el sistema busca al usuario por su correo único y extrae el hash almacenado en la base de datos.
  3. Posteriormente, se utiliza `passwordEncoder.matches(request.getPassword(), entity.getPasswordHash())`. La función hash de BCrypt toma la contraseña ingresada en texto plano, extrae el *salt* original del hash almacenado, realiza el cálculo matemático y compara los resultados de forma segura frente a ataques de temporización (*timing attacks*).

## 6. ¿Qué diferencia existe entre responder 401 Unauthorized y 403 Forbidden? Dé ejemplos de los endpoints implementados.

* **`401 Unauthorized` (No autenticado):** Indica que el cliente no ha proporcionado credenciales válidas o el token ha expirado/fue alterado. El sistema no sabe quién realiza la petición.
  * *Ejemplo implementado:* Consultar las tareas (`GET /api/v1/tasks`) enviando una cabecera `Authorization` con un token falso, modificado o ausente.
* **`403 Forbidden` (No autorizado / Prohibido):** Indica que el servidor conoce la identidad del usuario (está autenticado correctamente con un token válido), pero este no posee los privilegios o roles necesarios para ejecutar la acción solicitada.
  * *Ejemplo implementado:* Que un usuario con rol estándar (`USER`) intente acceder al resumen administrativo (`GET /api/v1/admin/summary`). El backend valida su token (es un usuario válido), pero al verificar su rol en los *claims*, deniega el acceso con `403`.

## 7. ¿Cómo impidieron que un usuario editara una tarea de otro, incluso con un JWT válido?

A nivel de arquitectura y código (`TaskServiceImpl.java` y `TaskRepository.java`), la restricción no depende de la interfaz de usuario ni de los parámetros que envíe el cliente, sino de la identidad validada en el backend:
1. El controlador extrae el ID del usuario directamente del token JWT firmado (`Long.valueOf(jwt.getSubject())`).
2. Las consultas a la base de datos no buscan únicamente por el ID de la tarea, sino de manera compuesta utilizando el ID del propietario:
   ```java
   taskRepository.findByIdAndUserId(id, userId)
   ```
3. Si un usuario con un JWT válido intenta modificar o consultar una tarea que pertenece a otra persona, el repositorio no la encuentra y el servicio lanza una excepción de tipo TaskNotFoundException, la cual el GlobalExceptionHandler traduce consistentemente a un código 404 Not Found. Esto previene ataques de tipo IDOR (Insecure Direct Object Reference) y evita filtrar la existencia de registros ajenos.

### 8. ¿Por qué ocultar botones en React no es suficiente para proteger los datos?

Ocultar componentes o botones en la interfaz de React (como la pestaña de "Resumen" o los botones de administración) es una medida puramente estética y de experiencia de usuario (UX).

Cualquier usuario con conocimientos básicos de desarrollo web puede utilizar herramientas de inspección del navegador, modificar el estado de la aplicación, o enviar peticiones HTTP directas utilizando herramientas como `curl` o Postman hacia los endpoints del backend (por ejemplo, `GET /api/v1/admin/summary`). Si el backend no validara las credenciales y los roles en cada petición, el sistema sería completamente vulnerable. Por esta razón, la seguridad real reside obligatoriamente en los filtros de Spring Security y en la validación de permisos en los servicios del servidor.


## 9. ¿Qué ventajas y limitaciones tuvo conservar el JWT en memoria en el frontend?

- **Ventajas de seguridad:**
  - **Mitigación de ataques XSS (Cross-Site Scripting):** Al almacenar el token únicamente en una variable de módulo en memoria RAM (`session.js`), se evita que scripts maliciosos o extensiones de terceros inyectadas en el navegador puedan extraerlo mediante `localStorage.getItem()` o `sessionStorage.getItem()`.
- **Limitaciones:**
  - **Pérdida de sesión al recargar:** Como la memoria RAM del navegador se limpia al refrescar o recargar la página (F5), el estado del token se pierde, obligando al usuario a volver a introducir sus credenciales en el login.
  - **Ausencia de persistencia automática:** Requiere que la aplicación gestione explícitamente el ciclo de vida de la sesión en memoria mediante contextos de React (`AuthProvider`).


## 10. ¿Qué limitación tiene el cierre de sesión sin revocación de tokens? ¿Qué solución propondría para un sistema real?

- **Limitación actual:** En nuestra implementación (y por diseño estándar de los JWT *stateless*), el cierre de sesión ocurre exclusivamente del lado del cliente (`session.js` borra el token de la memoria). Como el backend no mantiene estado de sesiones activas, un token JWT que ya haya sido emitido seguirá siendo criptográficamente válido ante el servidor hasta que se cumpla su tiempo de expiración (30 minutos). Si un atacante copió el token antes de que el usuario cerrara sesión, podría seguir utilizándolo hasta que expire.
- **Solución propuesta para un sistema real:**
  1. **Lista negra de tokens (Blacklist / Redis):** Al hacer logout, registrar el identificador único del token (`jti`) en una base de datos en caché de alta velocidad (como Redis) con un tiempo de vida igual al tiempo restante de expiración del token. El filtro de seguridad consultaría esta lista en cada petición.
  2. **Implementación de Refresh Tokens:** Utilizar tokens de acceso de corta duración (ej. 5 minutos) combinados con *refresh tokens* seguros almacenados en cookies HTTP-only con atributos `Secure` y `SameSite=Strict`, permitiendo revocar sesiones del lado del servidor de forma centralizada.


## 11. ¿Cuáles son las consecuencias de almacenar una llave de firma JWT dentro de GitHub?

Si la clave secreta (`JWT_SECRET`) se sube accidentalmente al repositorio público o privado de GitHub:
1. **Compromiso total de la seguridad:** Cualquier persona con acceso al historial de commits puede extraer la llave secreta.
2. **Falsificación de identidad (Token Forging):** Conociendo la clave secreta, un atacante puede generar sus propios tokens JWT válidos firmados con dicha clave, asignándose a sí mismo privilegios de administrador (ADMIN) o suplantando la identidad de cualquier usuario del sistema sin necesidad de conocer sus contraseñas.
3. **Pérdida de integridad:** Se invalidan por completo los principios de autenticación de la aplicación, obligando a revocar la clave, invalidar todos los tokens emitidos y reconfigurar las variables de entorno de producción.


## 12. ¿Qué pruebas del equipo demuestran la seguridad del CRUD y no solamente su funcionamiento correcto?

Las pruebas automatizadas de integración y seguridad implementadas (alineadas con la matriz del laboratorio) van más allá de verificar que una tarea se cree o se liste correctamente. Demuestran la robustez de los controles de seguridad mediante escenarios donde el sistema debe rechazar intencionalmente operaciones indebidas:
- **Prueba de acceso anónimo:** Verificar que consultar tareas sin la cabecera `Authorization` responde con `401 Unauthorized`.
- **Prueba de token manipulado:** Modificar un carácter del JWT y comprobar que el backend responde con `401 Unauthorized`.
- **Prueba de token expirado:** Simular la caducidad de la marca de tiempo y comprobar el rechazo con `401 Unauthorized`.
- **Prueba de aislamiento de datos (IDOR):** Iniciar sesión como Ana y verificar que al intentar consultar, modificar o eliminar una tarea perteneciente a Luis, el sistema responde consistentemente con `404 Not Found`, evitando la filtración o alteración de datos cruzados.
- **Prueba de control de roles:** Comprobar que un usuario con rol `USER` que intenta acceder al recurso administrativo `/api/v1/admin/summary` recibe una respuesta `403 Forbidden`, mientras que el rol `ADMIN` obtiene un `200 OK`.
