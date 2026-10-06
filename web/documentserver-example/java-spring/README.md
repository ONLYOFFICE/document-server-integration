# Java Spring integration

The Java Spring integration example is a Spring Boot application that lists files, opens them in ONLYOFFICE Docs, and saves them back through the callback handler. It is built on the [ONLYOFFICE Docs Integration SDK](https://api.onlyoffice.com/docs/docs-api/samples/language-specific-examples/java-integration-sdk/). Run it to see a complete integration working before you write your own.

> [!WARNING]
> This example is for testing only. It has no authentication, does not validate link parameters or save requests, and accepts requests from any site, so anyone who can reach it can read and change its files. Do not run it on a public server, and disable it before going to production.

## Prerequisites

- **ONLYOFFICE Docs**: [self-hosted](https://www.onlyoffice.com/download?from=api#docs-developer) or [cloud](https://www.onlyoffice.com/docs-registration?from=api).
- **Java**: version 11, which the example's `pom.xml` targets. See the [official website](https://www.oracle.com/java/technologies/downloads/#java11).
- **Apache Maven**: see the [official website](https://maven.apache.org/download.cgi).
- **Git**: see the [official website](https://git-scm.com/downloads).

## Step 1. Download the example

Clone the [example repository](https://github.com/ONLYOFFICE/document-server-integration/tree/main/web/documentserver-example/java-spring), go to the Java Spring example, and fetch its submodules:

```sh
git clone --depth 1 https://github.com/ONLYOFFICE/document-server-integration
cd document-server-integration/web/documentserver-example/java-spring
git submodule update --init --depth 1 .
```

## Step 2. Configure the connection

Open the `src/main/resources/application.properties` file and edit the following keys:

```ini
files.storage=
files.docservice.url.example=
docservice.url=http://documentserver/
docservice.security.key=
```

- `docservice.url`: the address of the server with ONLYOFFICE Docs installed. Replace `documentserver` with its name or IP address.
- `files.storage`: the folder where the example creates and stores files. If it is empty, the example uses the `documents` folder in the directory you run it from. You can set an absolute path. In this file, escape each backslash, for example, `D:\\folder`. The user running the example needs read and write permissions to this folder.
- `docservice.security.key`: must match the JWT settings of ONLYOFFICE Docs. JWT is enabled in ONLYOFFICE Docs by default, but the key is empty here, which disables JWT in the example. Set it to the [secret key](https://api.onlyoffice.com/docs/docs-api/additional-api/signature/) of your server.
- `files.docservice.url.example`: the address at which ONLYOFFICE Docs reaches the example, without a trailing slash, for example, `http://192.168.1.10:4000`. Set it if ONLYOFFICE Docs runs on another computer or in Docker, where the address you open in the browser, such as `localhost`, points somewhere else.

If you want to experiment with the editor configuration, modify the [parameters](https://api.onlyoffice.com/docs/docs-api/usage-api/advanced-parameters/) in the `createConfig` method of the `src/main/java/com/onlyoffice/integration/sdk/service/ConfigServiceImpl.java` file.

## Step 3. Build and run the example

```sh
mvn spring-boot:run
```

Open `http://localhost:4000` in your browser. To use another port, change `server.port` in `application.properties`. You will see the example's start page, where you can upload a file or create a new document, spreadsheet, presentation, or PDF form, and choose the test user and the editor language.

## Run with Docker

If you have [Docker](https://docs.docker.com/get-started/get-docker/) with Docker Compose, you can build and run the example in a container instead, without installing Java or Maven. This setup does not start ONLYOFFICE Docs, so complete step 2 first. From the example directory, run:

```sh
docker compose up --build
```

Open `http://localhost:4000` in your browser. The example folder is mounted into the container, so files are stored in its `documents` folder and changes to `application.properties` apply after a restart. Inside the container, `localhost` points to the container itself, so do not use it in `docservice.url`, and set `files.docservice.url.example` to an address at which ONLYOFFICE Docs reaches your computer.

## Troubleshooting

- **The document security token is not correctly formed**: JWT is disabled in the example (`docservice.security.key` is empty), or `docservice.security.key` does not match the secret of ONLYOFFICE Docs.
- **Download failed**: ONLYOFFICE Docs cannot reach the example. Set `files.docservice.url.example` in `application.properties` to an address that ONLYOFFICE Docs can resolve.

For more information, see the [documentation](https://api.onlyoffice.com/docs/docs-api/samples/language-specific-examples/java-spring-example/).
