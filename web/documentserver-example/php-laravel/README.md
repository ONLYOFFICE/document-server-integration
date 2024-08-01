## Overview

This is an example application written on PHP with [Laravel Framework](https://laravel.com/docs/11.x/installation#meet-laravel) that integrates ONLYOFFICE Docs.

> [!WARNING]  
> It is intended for testing purposes and demonstrating functionality of the editors. **DO NOT** use this integration example on your own server without proper code modifications. In case you enabled the test example, disable it before going for production.

## Environment Configuration

The root directory of the example application will contain a `.env.example` file that defines many common environment variables. You should copy the contents of the file to the `.env` file inside your project directory.
You can find the full list of environment variables [here](#environment-variables).

## Docker Installation

To get started, you need to install [Docker Desktop](https://www.docker.com/products/docker-desktop/).
The application provides you with docker files out-of-the-box, so you will just need to run a couple of shell commands to start the project. You can also change these files to your preferences.

### Initial Configuration

Download and extract the release archive in a directory.

```sh
$ cd /my/php-laravel/project
$ curl --output PHP-Laravel.Example.zip --location https://github.com/ONLYOFFICE/document-server-integration/releases/latest/download/PHP-Laravel.Example.zip
$ unzip PHP-Laravel.Example.zip
$ cd "PHP Laravel Example"
```
> [!WARNING]
> You should modify the `DOCUMENT_SERVER_JWT_SECRET` environment variable in your `.env` file as the `docker-compose.yml` uses it to set a JWT secret in OnlyOffice Docs Service.

### Running containers

Next, you can either run the `make compose-start` command that builds and starts the project, or you can execute the following commands manually:

- Build and start docker compose:
```sh
$ docker compose build && docker compose up -d
```

- Install the dependencies
```sh
$ docker compose exec example composer install
  && docker compose exec node npm install
  && docker compose exec node npm run build
```

- Generate an application key for the laravel instance
```sh
$ docker compose exec example php artisan key:generate
```

> [!NOTE]
> To stop the docker containers you should run the `make compose-stop` or `docker compose down` commands.

If the installation and configuration process has been successful, you can now visit `localhost:80` and try the example application.

> [!TIP]
> You can always change the configuration of the [nginx](docker/) web-server and the [port](docker-compose.yml) at which the application will run.

## Local Installation

Before diving into the example, you will need to install ONLYOFFICE Document Server (also known as Docs). Check the detailed guide to learn how to install it on [Windows](https://helpcenter.onlyoffice.com/installation/docs-developer-install-windows.aspx), [Linux](https://helpcenter.onlyoffice.com/installation/docs-developer-install-ubuntu.aspx), or [Docker](https://helpcenter.onlyoffice.com/installation/docs-developer-install-docker.aspx).

### Requirements

- PHP >= 8.2
- [Node.js (16+) and NPM](https://laravel.com/docs/11.x/vite#installing-node)
- [Laravel (11) Server Requirements](https://laravel.com/docs/11.x/deployment#server-requirements)

Once you have everything installed, download the release archive and extract it in your preferred directory.

```sh
$ cd /path/to/my/projects
$ curl --output PHP.Example.zip --location https://github.com/ONLYOFFICE/document-server-integration/releases/latest/download/PHP.Example.zip
$ unzip PHP.Example.zip
$ cd "PHP Laravel Example"
```

Now, you can either run `make install` or execute the following commands manually:

- Install composer dependencies
```sh
$ composer install
```

- Install the application's frontend dependencies and build the assets
```sh
$ npm install && npm run build
```

- Generate an application key for the laravel instance
```sh
$ php artisan key:generate
```

If the installation and configuration process has been successful, you can now visit your server's address and try the example application.

## Post Installation

In case the example and Document Server are installed on different computers, make sure that your server with the example installed has access to the Document Server with the address which you specify instead of **documentserver** in the configuration files. 

Make sure that the Document Server has access to the server with the example installed with the address which you specify instead of **example.com** in the configuration files.

## Environment variables

The following table shows the environment variables that is used to configure the example application.

| Name                          | Description                                                             | Example                 |
| ----------------------------- | ----------------------------------------------------------------------- | ----------------------- |
| `USER`                        | The user name in the system.                                                     | `user` |
| `UID`                         | The user ID number (UID) in the system.                                 | `1000` |
| `DOCUMENT_STORAGE_PUBLIC_URL` | The URL address used by the client to communicate with the server.      | `http://localhost`      |
| `DOCUMENT_STORAGE_PRIVATE_URL`| The URL address used by the Document Server to communicate with the server. | `http://proxy`          |
| `DOCUMENT_SERVER_PUBLIC_URL`  | The URL address used by the client to communicate with the Document Server. | `http://localhost:8080` |
| `DOCUMENT_SERVER_PRIVATE_URL` | The URL address used by the server to communicate with the Document Server. | `http://proxy:8080`     |
| `DOCUMENT_SERVER_JWT_SECRET`  | JWT authorization secret.                                               | `your-256-bit-secret`   |

## Troubleshooting

Check out the [Laravel documentation](https://laravel.com/docs/11.x/deployment#server-configuration) page in case you are having installation or configuration problems with the framework.

## Security Info

Please keep in mind the following security aspects when you are using test examples:

- There is no protection of the storage from unauthorized access since there is no need for authorization.
- There are no checks against parameter substitution in links, since the parameters are generated by the code according to the pre-arranged scripts.
- There are no data checks in requests of saving the file after editing, since each test example is intended for requests only from ONLYOFFICE Document Server.
- There are no prohibitions on using test examples from other sites, since they are intended to interact with ONLYOFFICE Document Server from another domain.
