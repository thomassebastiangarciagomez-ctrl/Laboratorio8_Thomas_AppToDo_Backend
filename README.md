# Laboratorio8_Thomas_AppToDo_Backend
Este repositorio hace parte del laboratorio 8 de la materia de DOSW, sección BackEnd de la aplicación propuesta AppToDo

| Name                            | GitHub Username                   |
|---------------------------------|-----------------------------------|
| Daniel Santiago Morales Perdomo | `Fukuyaku88`                      |
| Edgar Daniel Ruiz Patiño        | `DaniRuiz11`                      |
| Miguel Ángel Acero Laverde      | `Miguelim101`                     |
| Thomas Sebastian García Gomez   | `thomassebastiangarciagomez-ctrl` |

## Parte 5: Preparar calidad del backend

En esta parte, para poder realizar las pipelines de manera adecuada, primero, verificar que el codigo del backend tenga la calidad suficiente, para esto nos proponen, a parte de tener la dependencia de Jacoco que ya teniamos, incluir una nueva dependencia llamada SpotBugs.

Esta nueva dependencia se encarga de detectar diferentes patrones de fallos, detectando bugs que pueden llegar a ser importantes al momento de construir software. En nuestro caso, detecto 6 bugs de media importancia, sin embargo, es importante correguirlos para poder realizar esta parte del laboratorio de manera correcta.

Gracias a la utilizacion de IA, se detectaron los bugs de manera adecuada y las soluciones a los mismos, donde en 2 casos especificos fueron "falsos positivos", debido a que estos son comportamiento del codigo actual y no deben de ser modificados. Por esta misma razon se agrego el siguiente archivo .xml para evitar algun error en la contruccion del proyecto.

```xml
<FindBugsFilter>
  <Match>
    <Class name="edu.eci.dosw.todo.controller.TaskController"/>
    <Bug pattern="EI_EXPOSE_REP2"/>
  </Match>
  <Match>
    <Class name="edu.eci.dosw.todo.entity.TaskEntity"/>
    <Bug pattern="EI_EXPOSE_REP,EI_EXPOSE_REP2"/>
  </Match>
</FindBugsFilter>
```

Y de igual forma, debido a esta integracion, se debe agregar en la seccion de configuraciones de SpotBugs en el pom.xml el siguiente codigo para que se configure todo de manera adecuada.

```xml
<threshold>Medium</threshold>
<excludeFilterFile>spotbugs-exclude.xml</excludeFilterFile>
```

Al realizar todo este procedimiento, para ejecutar las pruebas y la calidad de las mismas se debe ejecutar en consola dentro de la carpeta de backend el siguiente comando. Esto generara los archivos necesarios para esta parte del laboratorio.

```bash
mvn -B -ntp clean verify
```

Con esto, el archivo se construira de manera adecuada, lo que nos indica que el codigo es el adecuado. Con base en esto, dentro del directorio de docs/ se encuentran el analisis Jacoco y la carpeta de SpotBugs surefire-reports/ que es solicitada en esta parte del laboratorio.

![PruebasCalidad](docs/ci-cd-evidencias/PruebaCalidad.png)

## Parte 7 Etapa A3. Preparar imágenes de Docker (build multietapa):

Se integran los nuevos archivos, estos lo que hacen es construir la imagen en docker, de tal forma que es importante realizar algunos pequeños cambios en el backend, principalmente en utilizar Java 21 y no 17. Esta seria la integracion.

```dockerfile
# Etapa 1: compilación reproducible con Maven
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
# Las pruebas ya se ejecutan en CI; no se saltan las validaciones del pipeline.
RUN mvn -B -ntp -DskipTests package

# Etapa 2: runtime compacto (sin Maven ni fuentes)
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

Tambien se tienen que realizar las integraciones correspondientes en el frontend, en donde lo que se logra observar al ejecutar en consola, dentro de cada uno de los comandos correspondientes, los cuales son

```bash
docker build -t todo-api:local ./backend
docker build -t todo-web:local ./frontend
docker images | grep todo
```

Al ejecutar esto y realizar la consulta de las construicciones hechas en docker, podemos observar que se crearon correctamente en la siguiente imagen.

![ConstruccionDocker](docs/ci-cd-evidencias/ConstruccionDocker.png)

## Parte 8 Etapa A4. Crear el workflow de integración:

En esta seccion, lo que se debe realizar es integrar los workflow en github, el documento propuesto es para un repositorio general, sin emabrgo, desde el principio hemos manejado 2 repositorios separados, de tal forma que para la integracion adecuada entre ambos, tenemos que tener el archivo .yml en cada repositorio con sus correspondientes configuraciones. Teniendo esto en cuenta, en cada repositorio hay que integrar lo siguiente.

En backend:

![ActionBack](docs/ci-cd-evidencias/ActionBack.png)

En frontend:

![ActionFront](docs/ci-cd-evidencias/ActionFront.png)

Al crear la nueva rama y subirla a github, cuando realicemos un pull request va a generarse una accion la cual va a comprobar diferentes pruebas para comprobar que efectivamente se puede realizar un merge.

## Parte 9 Etapa B1. Definir el stack productivo del laboratorio:

En esta parte, tenemos que configurar la infraestructura necesaria para subir todo a la nube de manera adecuada, para esto, se nos brindan los codigos necesarios, donde se les realizaron algunas modificaciones, debido a que en el laboratorio se concidera que se maneja 1 solo repositorio, pero aqui manejamos 2.

Al realizar estos cambios, tenemos que ejecutar desde powershell los siguientes comandos para realizar las pruebas necesarias y que se configure todo de manera adecuada (es importante previamente haber realizado el Pull Request a main y verificar que los paquetes se hayan creado correctamente).

```bash
docker compose --env-file .env -f docker-compose.prod.yml config --quiet
docker compose --env-file .env -f docker-compose.prod.yml pull
docker compose --env-file .env -f docker-compose.prod.yml up -d
docker compose --env-file .env -f docker-compose.prod.yml ps
```

Al ejecutar estos comandos en nuestra consola y despues de realizar un par de correcciones necesarias, la siguiente imagen es el resultado que debe arrojar los mismos.

![imagenInfra](docs/ci-cd-evidencias/imagenInfra.png)

De igual forma para verificar que todo salio de manera adecuada, podemos ver desde docker que se creo una nueva imagen que esta asociada a nuestro proyecto.

![imagenDocker](docs/ci-cd-evidencias/imagenDocker.png)

## Parte 10 Etapa C1. Preparar Azure for Students:

