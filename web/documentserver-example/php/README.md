# PHP integration

The PHP integration example is a small plain PHP application, built without a framework, that lists files, opens them in ONLYOFFICE Docs, and saves them back through the callback handler. Run it to see a complete integration working before you write your own.

> [!WARNING]
> This example is for testing only. It has no authentication, does not validate link parameters or save requests, and accepts requests from any site, so anyone who can reach it can read and change its files. Do not run it on a public server, and disable it before going to production.

## Prerequisites

- **ONLYOFFICE Docs**: [self-hosted](https://www.onlyoffice.com/download?from=api#docs-developer) or [cloud](https://www.onlyoffice.com/docs-registration?from=api).
- **PHP**: see the [official website](https://www.php.net/downloads).
- **Composer**: see the [official website](https://getcomposer.org/download/).
- **Git**: see the [official website](https://git-scm.com/downloads).

## Step 1. Download the example

Clone the [example repository](https://github.com/ONLYOFFICE/document-server-integration/tree/main/web/documentserver-example/php), go to the PHP example, and fetch its submodules:

```sh
git clone --depth 1 https://github.com/ONLYOFFICE/document-server-integration
cd document-server-integration/web/documentserver-example/php
git submodule update --init --depth 1 .
```

## Step 2. Configure the connection

The example has no configuration file. It reads its settings from environment variables, so set them in the terminal where you will run the example in the next step:

```sh
export DOCUMENT_SERVER_PUBLIC_URL=http://documentserver
export STORAGE_PATH=storage
export JWT_SECRET=secret
export EXAMPLE_URL=http://example.com:9000
```

On Windows, use `set NAME=value` in Command Prompt or `$env:NAME = "value"` in PowerShell instead of `export`.

- `DOCUMENT_SERVER_PUBLIC_URL`: the address of the server with ONLYOFFICE Docs installed. Replace `documentserver` with its name or IP address. The default is `http://documentserver`.
- `STORAGE_PATH`: the folder where the example creates and stores files. The default is `storage`. A relative path is resolved from the example directory, and you can also set an absolute path. The user running the example needs read and write permissions to this folder.
- `JWT_SECRET`: must match the JWT secret of ONLYOFFICE Docs. If it is empty, which is the default, the example disables JWT. JWT is enabled in ONLYOFFICE Docs by default, so set `JWT_SECRET` to the [secret key](https://api.onlyoffice.com/docs/docs-api/additional-api/signature/) of your server.
- `EXAMPLE_URL`: the address at which ONLYOFFICE Docs reaches the example. Set it if ONLYOFFICE Docs runs on another computer or in Docker, where the address you open in the browser, such as `localhost`, points somewhere else.

If you want to experiment with the editor configuration, modify the [parameters](https://api.onlyoffice.com/docs/docs-api/usage-api/advanced-parameters/) in the `$config` array of the `src/views/DocEditorView.php` file.

## Step 3. Install the dependencies and run the example

```sh
composer install --no-dev
php -S 0.0.0.0:9000
```

Open `http://localhost:9000` in your browser. You will see the example's start page, where you can upload a file or create a new document, spreadsheet, presentation, or PDF form.

## Run with Docker

If you have Docker with Docker Compose, you can run the example together with its own ONLYOFFICE Docs and an nginx proxy instead. You do not need a separate ONLYOFFICE Docs server for this. From the example directory, run:

```sh
docker compose up --build --detach
```

Open `http://localhost` in your browser. The environment variables are set in the `docker-compose.yml` file. JWT is enabled with the `your-256-bit-secret` secret. If you change it, change `JWT_SECRET` for both the `documentserver` and `example` services.

## Troubleshooting

- **The document security token is not correctly formed**: JWT is disabled in the example (`JWT_SECRET` is empty), or `JWT_SECRET` does not match the secret of ONLYOFFICE Docs.
- **Download failed**: ONLYOFFICE Docs cannot reach the example. Set `EXAMPLE_URL` to an address that ONLYOFFICE Docs can resolve, and restart the example.

For more information, see the [documentation](https://api.onlyoffice.com/docs/docs-api/samples/language-specific-examples/php-example/).
