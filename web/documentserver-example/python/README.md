## Overview

This example will help you integrate ONLYOFFICE Docs into your web application written on Python.

> [!WARNING]  
> It is intended for testing purposes and demonstrating functionality of the editors. **DO NOT** use this integration example on your own server without proper code modifications. In case you enabled the test example, disable it before going for production.

## Installation

The Python example offers various installation options, but we highly recommend using Docker for this purpose.

### Using Docker

You can run this example in Docker on a local machine or can be deployed on a virtual machine on a cloud infrastructure provider.

To run the example using [Docker](https://docker.com), you will need [Docker Desktop 4.17.0](https://docs.docker.com/desktop) for a local machine or [Docker Engine 20.10.23](https://docs.docker.com/engine) on the cloud with [Docker Compose 2.15.1](https://docs.docker.com/compose). Additionally, you might want to consider installing [GNU Make 4.4.1](https://gnu.org/software/make), although it is optional. These are the minimum versions required for the tools.

Once you have everything installed, download the release archive and unarchive it.

```sh
$ curl --output Python.Example.zip --location https://github.com/ONLYOFFICE/document-server-integration/releases/latest/download/Python.Example.zip
$ unzip Python.Example.zip
```

Then open the example directory and [up containers](./Makefile#L38).

```sh
$ cd "Python Example"
$ make compose-prod
```

By default, the server starts at `localhost:80` on the machine that you are using. You will see a page with the welcome to ONLYOFFICE Document Editors.

To configure the example, you can edit the environment variables in [`compose-base.yml`](./compose-base.yml). See [below](#configuration) for more information about environment variables.

### On Local Machine

Before diving into the example, you will need to install ONLYOFFICE Document Server (also known as Docs). Check the detailed guide to learn how to install it on [Windows](https://helpcenter.onlyoffice.com/installation/docs-developer-install-windows.aspx), [Linux](https://helpcenter.onlyoffice.com/installation/docs-developer-install-ubuntu.aspx), or [Docker](https://helpcenter.onlyoffice.com/installation/docs-developer-install-docker.aspx).

To run the example on your local machine, you will need [Python 3.11.4](https://python.org) with [pip 23.1.2](https://pip.pypa.io). Additionally, you might want to consider installing [GNU Make 4.4.1](https://gnu.org/software/make), although it is optional. These are the minimum versions required for the tools.

Once you have everything installed, download the release archive and unarchive it.

```sh
$ curl --output Python.Example.zip --location https://github.com/ONLYOFFICE/document-server-integration/releases/latest/download/Python.Example.zip
$ unzip Python.Example.zip
```

Then open the example directory, [install dependencies](./Makefile#L13), and [start the server](./Makefile#L21).

```sh
$ cd "Python Example"
$ make prod
$ make server-prod
```
The `make prod` command installs the dependancies into the Python environment you have activated. The `make server-prod` starts up the python server using the `manage.py` file. More details of these commands are in the `Makerfile`.

By default, the server starts at `0.0.0.0:8000`. To configure the example, you can pass the environment variables before the command that starts the server. See [below](#configuration) for more information about environment variables. You will need to configure the example that will be on `0.0.0.0:8000` to access documentserver 

## Post Installation

In case the example and Document Server are installed on different computers, make sure that your server with the example installed has access to the Document Server with the address which you specify instead of **documentserver** in the configuration files. 

Make sure that the Document Server has access to the server with the example installed with the address which you specify instead of **example.com** in the configuration files.

## Configuration

The example is configured by changing environment variables.

| Name                          | Description                                                             | Example                 |
| ----------------------------- | ----------------------------------------------------------------------- | ----------------------- |
| `DEBUG`                       | Disable or enable debug mode.                                           | `false`                 |
| `ADDRESS`                     | The address where the server should be started.                         | `0.0.0.0`               |
| `PORT`                        | The port on which the server should be running.                         | `80`                    |
| `DOCUMENT_SERVER_PRIVATE_URL` | The URL through which the server will communicate with Document Server. | `http://proxy:8080`     |
| `DOCUMENT_SERVER_PUBLIC_URL`  | The URL through which a user will communicate with Document Server.     | `http://localhost:8080` |
| `EXAMPLE_URL`                 | The URL through which Document Server will communicate with the server. | `http://proxy`          |
| `JWT_SECRET`                  | JWT authorization secret. Leave blank to disable authorization.         | `your-256-bit-secret`   |

## Security Info

Please keep in mind the following security aspects when you are using test examples:

- There is no protection of the storage from unauthorized access since there is no need for authorization.
- There are no checks against parameter substitution in links, since the parameters are generated by the code according to the pre-arranged scripts.
- There are no data checks in requests of saving the file after editing, since each test example is intended for requests only from ONLYOFFICE Document Server.
- There are no prohibitions on using test examples from other sites, since they are intended to interact with ONLYOFFICE Document Server from another domain.
