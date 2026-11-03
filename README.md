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
