# Kepler

Kepler is a Habbo Hotel emulator that is designed to fully emulate the v14 version from 2007 era. 
The server is written in Java and using various libraries, such as Netty, an asynchronous networking library, Log4j and the Apache commons libraries.

It is quite possibly the most complete v14 Habbo Hotel released to date, and has been in development since January 2018.

# Screenshots

(Hotel view)

![https://i.imgur.com/8eFvtdA.png](https://i.imgur.com/8eFvtdA.png)

(Automatic rare cycler)

![https://i.imgur.com/8RTFFqD.png](https://i.imgur.com/8RTFFqD.png)

(Camera)

![https://i.imgur.com/emseVbU.png](https://i.imgur.com/emseVbU.png)

(BattleBall)

![https://i.imgur.com/a3MgkzU.png](https://i.imgur.com/a3MgkzU.png)

![https://i.imgur.com/eUGmcwR.png](https://i.imgur.com/eUGmcwR.png)

(Chess)

![https://i.imgur.com/xundc8M.png](https://i.imgur.com/xundc8M.png)

(Tic Tac Toe)

![https://i.imgur.com/tTG5SVE.png](https://i.imgur.com/tTG5SVE.png)

# Download

Download the latest development build from the [releases page](https://github.com/Quackster/Kepler/releases).

### Requirements

To be honest, this server doesn't require much. I'd argue that the MariaDB server is more resource demanding than the emulator itself. 

- JDK >= 17
- MariaDB server

# Installation

Install MariaDB server, connect to the database server and import Kepler.sql (located in /tools/Kepler.sql).

Download the latest development build from the [releases page](https://github.com/Quackster/Kepler/releases) and rename the files to remove the short build hash version, for convenience. 

Install any JDK version that is equal or above >= 17 to run the jar files.

Open run.sh (Linux) or run.bat (Windows) to start Kepler.

❗ Once registered as an admin, make yourself admin by setting your ``rank`` to 7 in the ``users`` table.

As for the client, you can find version 14 DCRs [here](https://web.archive.org/web/20220724030154/https://raw.githubusercontent.com/Quackster/Kepler/master/tools/Quackster_v14.zip).

Setup the loader files on a web server, and once Kepler is started, ensure the loader is connecting to the correct IP and ports for both the standard connection and MUS connection. The MUS connection is used for the camera.

# Docker installation

Install [Docker](https://docs.docker.com/engine/install/) and [git](https://git-scm.com/downloads) (optional) on your device.

### 1. Clone repository

```shell
git clone https://github.com/Quackster/Kepler.git
```

_You can also [download](https://github.com/Quackster/Kepler/archive/refs/heads/master.zip) this repository and unzip it._

### 2. Configure variables

Copy `.env.example` file to `.env` :

```shell
cp .env.example .env
```

You can now configure all variables in `.env` file with values needed.

_Don't change `MYSQL_HOST` except if you change the name of the service `mariadb` in Docker compose file._

_You neither should change `MYSQL_PORT`._

### 3. Inicio de Kepler

Si prefieres utilizar **Docker** directamente (por ejemplo en EasyPanel) sin
`docker compose`, primero construye la imagen:

```shell
docker build -t kepler .
```

El Dockerfile utiliza una imagen oficial de **Gradle** para compilar el
servidor sin necesidad de descargar ficheros adicionales desde GitHub, lo que
facilita el despliegue en entornos con restricciones de red como EasyPanel.

Inicia un contenedor de MariaDB (puedes hacerlo también desde EasyPanel):

```shell
docker run -d --name kepler-db \
  -e MARIADB_ROOT_PASSWORD=veryverysecret \
  -e MYSQL_DATABASE=kepler \
  -e MYSQL_USER=kepler \
  -e MYSQL_PASSWORD=verysecret \
  mariadb:11.4
```

Por último arranca Kepler enlazándolo con la base de datos y exponiendo los
puertos necesarios:

```shell
docker run -d --name kepler --link kepler-db:mariadb \
  -p 12321:12321 -p 12309:12309 -p 12322:12322 \
  -e MYSQL_HOSTNAME=mariadb \
  kepler
```

Con EasyPanel puedes crear estos contenedores desde su interfaz gráfica y
configurar las variables de entorno anteriores. Para simplificar aún más el
proceso, este repositorio incluye un script `deploy.sh` que ejecuta todos los
pasos anteriores de forma automática:

```shell
./deploy.sh
```

El script construye la imagen, inicia MariaDB si no existe, importa
automáticamente `tools/kepler.sql` la primera vez y arranca Kepler ligado a
dicha base de datos.

Cuando subes este repositorio a **EasyPanel** y seleccionas la opción
"Dockerfile", la imagen resultante utilizará el script `start.sh` para poner en
marcha el servidor. Al arrancar el contenedor verás en los logs un mensaje con
la URL de acceso. Por defecto podrás conectarte a
`http://TU_IP_o_DOMINIO:12321` a menos que hayas modificado el puerto con la
variable `SERVER_PORT`.

### Docker FAQ

#### Reset MariaDB database

You need to first stop Kepler, then remove MariaDB volume :

```shell
docker compose down && docker volume rm kepler-mariadb
```

You can now start Kepler again, database will be wiped out !

## License

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.


## Cloning this repository

```
$ git clone --recursive https://github.com/Quackster/Kepler
```

**or**

```
$ git clone https://github.com/Quackster/Kepler
$ git submodule update --init --recursive
```

# Thanks to

* Hoshiko
* ThuGie
* Ascii
* Lightbulb
* Raptosaur
* Romuald
* Glaceon
* Nillus
* Holo Team
* wackfx
* Meth0d
* office.boy
* Leon Hartley
* Alito
* wackfx
