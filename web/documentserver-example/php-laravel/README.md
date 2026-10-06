# PHP Laravel integration

The PHP Laravel integration example is a small Laravel application that lists files, opens them in ONLYOFFICE Docs, and saves them back through the callback handler. Its Docker setup also starts ONLYOFFICE Docs, so you can see a complete integration working before you write your own.

> [!WARNING]
> This example is for testing only. It has no authentication, does not validate link parameters or save requests, and accepts requests from any site, so anyone who can reach it can read and change its files. Do not run it on a public server, and disable it before going to production.

## Prerequisites

- **Docker and Docker Compose**: see the [official website](https://docs.docker.com/get-started/get-docker/).
- **Git**: see the [official website](https://git-scm.com/downloads).
- **ONLYOFFICE Docs**: optional, because the Docker setup starts its own. To use your own server, get the [self-hosted](https://www.onlyoffice.com/download?from=api#docs-developer) or [cloud](https://www.onlyoffice.com/docs-registration?from=api) version.

## Step 1. Download the example

Clone the [example repository](https://github.com/ONLYOFFICE/document-server-integration/tree/main/web/documentserver-example/php-laravel), go to the PHP Laravel example, and fetch its submodules:

```sh
git clone --depth 1 https://github.com/ONLYOFFICE/document-server-integration
cd document-server-integration/web/documentserver-example/php-laravel
git submodule update --init --depth 1 .
```

## Step 2. Configure the connection

Create the `.env` file from the template:

```sh
cp .env.example .env
```

The default values work as is: Docker Compose starts ONLYOFFICE Docs next to the example and passes it the same JWT secret. To connect your own ONLYOFFICE Docs server instead, edit the following variables in `.env`:

```ini
DOCUMENT_SERVER_PUBLIC_URL=http://localhost:8080
DOCUMENT_SERVER_PRIVATE_URL=http://proxy:8080
DOCUMENT_STORAGE_PRIVATE_URL=http://proxy
DOCUMENT_SERVER_JWT_SECRET=secret
```

- `DOCUMENT_SERVER_PUBLIC_URL` and `DOCUMENT_SERVER_PRIVATE_URL`: the address of your ONLYOFFICE Docs server.
- `DOCUMENT_STORAGE_PRIVATE_URL`: the address at which ONLYOFFICE Docs reaches the example. The name `proxy` resolves only inside the Docker network.
- `DOCUMENT_SERVER_JWT_SECRET`: the [secret key](https://api.onlyoffice.com/docs/docs-api/additional-api/signature/) of your server.

If you want to experiment with the editor configuration, modify the [parameters](https://api.onlyoffice.com/docs/docs-api/usage-api/advanced-parameters/) in the `app/Models/Editor/Editor.php` file.

## Step 3. Build and run the example

```sh
docker compose up -d --build
```

Open `http://localhost` in your browser. You will see the example's start page, where you can upload a file or create a new document, spreadsheet, presentation, or PDF form.

The example's files, including `.env`, are copied into a Docker volume on the first start. After you change them, run `docker compose down -v` and start the example again.

## Troubleshooting

- **The document security token is not correctly formed**: JWT is disabled in the example (`DOCUMENT_SERVER_JWT_SECRET` is empty), or `DOCUMENT_SERVER_JWT_SECRET` does not match the secret of ONLYOFFICE Docs. If you changed it after the first start, recreate the volume with `docker compose down -v`.
- **Download failed**: ONLYOFFICE Docs cannot reach the example. Set `DOCUMENT_STORAGE_PRIVATE_URL` in `.env` to an address that ONLYOFFICE Docs can resolve.
- **Laravel installation or configuration problems**: see the [Laravel documentation](https://laravel.com/docs/11.x/deployment#server-configuration).

For more information, see the [documentation](https://api.onlyoffice.com/docs/docs-api/samples/language-specific-examples/php-laravel-example/).
