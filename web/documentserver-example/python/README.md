# Python integration

The Python integration example is a small Django application that lists files, opens them in ONLYOFFICE Docs, and saves them back through the callback handler. Run it to see a complete integration working before you write your own.

> [!WARNING]
> This example is for testing only. It has no authentication, does not validate link parameters or save requests, and accepts requests from any site, so anyone who can reach it can read and change its files. Do not run it on a public server, and disable it before going to production.

## Prerequisites

- **ONLYOFFICE Docs**: [self-hosted](https://www.onlyoffice.com/download?from=api#docs-developer) or [cloud](https://www.onlyoffice.com/docs-registration?from=api).
- **Python**: version 3.11.4 or later, see the [official website](https://www.python.org/downloads/).
- **libmagic**: required by the `python-magic` package, see its [installation instructions](https://github.com/ahupp/python-magic#installation).
- **Git**: see the [official website](https://git-scm.com/downloads).

## Step 1. Download the example

Clone the [example repository](https://github.com/ONLYOFFICE/document-server-integration/tree/main/web/documentserver-example/python), go to the Python example, and fetch its submodules:

```sh
git clone --depth 1 https://github.com/ONLYOFFICE/document-server-integration
cd document-server-integration/web/documentserver-example/python
git submodule update --init --depth 1 .
```

## Step 2. Configure the connection

The example has no configuration file. It reads its settings from environment variables, so set them in the terminal where you will run the example in the next step:

```sh
export DOCUMENT_SERVER_PUBLIC_URL=http://documentserver
export STORAGE_PATH=storage
export JWT_SECRET=secret
export EXAMPLE_URL=http://example.com:8000
```

On Windows, use `set NAME=value` in Command Prompt or `$env:NAME = "value"` in PowerShell instead of `export`.

- `DOCUMENT_SERVER_PUBLIC_URL`: the address of the server with ONLYOFFICE Docs installed. Replace `documentserver` with its name or IP address. If the example has to reach ONLYOFFICE Docs at a different address than the browser does, also set `DOCUMENT_SERVER_PRIVATE_URL` to that address.
- `STORAGE_PATH`: the folder where the example creates and stores files. The default is the `storage` folder inside the example directory. You can set an absolute path. The user running the example needs read and write permissions to this folder.
- `JWT_SECRET`: must match the JWT settings of ONLYOFFICE Docs. JWT is enabled in ONLYOFFICE Docs by default but disabled in the example while this variable is empty, so set it to the [secret key](https://api.onlyoffice.com/docs/docs-api/additional-api/signature/) of your server.
- `EXAMPLE_URL`: the address at which ONLYOFFICE Docs reaches the example, without a trailing slash. Set it if ONLYOFFICE Docs runs on another computer or in Docker, where the address you open in the browser, such as `localhost`, points somewhere else.

If you want to experiment with the editor configuration, modify the [parameters](https://api.onlyoffice.com/docs/docs-api/usage-api/advanced-parameters/) in the `edit` function of the `src/views/actions.py` file.

## Step 3. Install the dependencies and run the example

Create a virtual environment, install the dependencies into it, and start the server:

```sh
python3 -m venv .venv
source .venv/bin/activate
pip install .
python manage.py runserver 0.0.0.0:8000
```

On Windows, run `python -m venv .venv` and `.venv\Scripts\activate` instead of the first two commands.

Open `http://localhost:8000` in your browser. You will see the example's start page, where you can upload a file or create a new document, spreadsheet, presentation, or PDF form.

## Run with Docker

The example also includes Docker Compose files that start it together with its own ONLYOFFICE Docs, so you only need [Docker](https://docs.docker.com/get-started/get-docker/) with Docker Compose. After step 1, run in the example directory:

```sh
docker compose --file compose-base.yml --file compose-prod.yml up --detach --build
```

Open `http://localhost` in your browser. To change the settings from step 2, edit the `environment` section of the `example` service in the `compose-base.yml` file. If you change `JWT_SECRET` there, change it for the `documentserver` service as well.

## Troubleshooting

- **The document security token is not correctly formed**: `JWT_SECRET` is not set, so JWT is disabled in the example, or its value does not match the secret of ONLYOFFICE Docs.
- **Download failed**: ONLYOFFICE Docs cannot reach the example. Set `EXAMPLE_URL` to an address that ONLYOFFICE Docs can resolve, and restart the example.

For more information, see the [documentation](https://api.onlyoffice.com/docs/docs-api/samples/language-specific-examples/python-example/).
