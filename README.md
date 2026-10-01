# Plataforma de creación, gestión y seguimiento de Autoevaluación Institucional y Plan de Mejoramiento Institucional
El presente proyecto tiene como finalidad el desarrollo de un sistema de creación, gestión y seguimiento para los procesos de Autoevaluación Institucional y Plan de Mejoramiento Institucional orientado al uso de la Secretaria de Educación Municipal y las instituciones vinculadas.
___
# Índice
- [Tecnologías Empleadas](#tecnologías-empleadas)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Endpoints y rutas](#endpoints-y-rutas)
- [Documentación Técnica](#documentación-técnica)
___
# Tecnologías empleadas
## Backend
- Spring Boot 4.1
## Frontend
- Vue 3.5.32
## Requisitos
- Java 21 LTS
- Node.JS 22.18
- Typescript 6.0
___
# Endpoints y rutas
Estos son los endpoints estructurados y referenciados desde nuestro Backend, se especifica que entrega y/o espera recibir cada una de las rutas junto con el metodo HTTP que se debe utilizar para su correcto funcionamiento.
P.D Estos endpoints se indican en este README para la fecha de: 1 de Octube de 2026, los metodos utilizados, asi como respuestas y cuerpo solicitado para cada ruta pueden variar a lo largo del desarrollo, por favor verificar con código y asegurarse de que la ultima fecha especificada de este texto sea cercana a los ultimos cambios realizados en la rama master.
## Autenticación
```autenticacion
{RutaDeBackend}/api/auth/*
    >/secretary/*
        >/login
    >/establishment/*
        >/login
```
Estas rutas son relacionadas a rutas desprotegidas que no tienen ningun tipo de verificacion JWT para su acceso. /
Se especifica en estas rutas inicio de sesion para los 2 tipos de usuario abarcados en el backend, permitiendo acceder al proceso de autenticacion en sistema.
### Cuerpo y respuesta:
**Login de secretaria**
``` body_esperado
POST http://{RutadeBackend}/api/auth/secretary/login
Content-Type: application/json

{ "email": "secretaria@prueba.com", "password": "admin123" }

Debe retornar:
Respuesta 200 OK
{"token": "TokenDeSesion", "id": "UUID", "name": "NombreDeUsuario", "role": "SECRETARY"}
```
**Login de Institucion**
```body_esperado
POST http://localhost:8080/api/auth/establishment/login
Content-Type: application/json

{ "daneCode": "123456789012", "password": "admin123" }

Debe retornar:
{"token": "TokenDeSesion", "id": "UUID", "name": "NombreDeUsuario", "role": "ESTABLISHMENT"}
```
## Gestion de usuarios
```gestion_usuarios
{RutaDeBackend}/api/me
```
Las rutas externas a la autenticacion, requieren en su totalidad un token para su correcto acceso del tipo *Bearer Token*, estas rutas referencian todo lo relacionado a manejo de usuarios dentro del sistema, como extraer, ingresar e editar informacion.
### Cuerpo y respuesta:
**Extraer informacion de la sesion**
```body_esperado
GET http://localhost:8080/api/me
Authorization: Bearer <token>

Debe retornar
{"id": "UUID", "name": "NombreDeUsuario", "role": "Rol", "email": "correo"}
solo SI eres institucion, se incluye en la respuesta:
{"daneCode": "Codigo", "rector": "nombreRector"}
```
___
# Estructura del Proyecto
```estructura
├───backend
│   ├───gradle
│   │   └───wrapper
│   └───src
│       ├───main
│       │   ├───java
│       │   │   └───com
│       │   │       └───sem
│       │   │           └───pmiautoevaluacion
│       │   │               ├───auth
│       │   │               │   ├───controller
│       │   │               │   ├───dto
│       │   │               │   ├───security
│       │   │               │   └───service
│       │   │               ├───shared
│       │   │               │   ├───config
│       │   │               │   ├───enums
│       │   │               │   └───exception
│       │   │               └───users
│       │   │                   ├───controller
│       │   │                   ├───dto
│       │   │                   ├───entity
│       │   │                   ├───repository
│       │   │                   └───service
│       │   └───resources
│       │       └───db
│       │           └───migration
│       └───test
│           └───java
│               └───com
│                   └───sem
│                       └───pmiautoevaluacion
├───frontend
│   ├───public
│   └───src
│       ├───assets
│       ├───components
│       │   └───icons
│       ├───router
│       └───views
└── README.md
```
___
# Documentación Técnica
La documentación técnica, diagramas y demás artefactos de análisis y diseño del proyecto se encuentran disponibles en la wiki del proyecto:
[Wiki](https://github.com/edben110/AutoevaluacionYPMI/wiki)