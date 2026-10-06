# Ruby integration

The Ruby integration example is a small Ruby on Rails application that lists files, opens them in ONLYOFFICE Docs, and saves them back through the callback handler. Run it to see a complete integration working before you write your own.

> [!WARNING]
> This example is for testing only. It has no authentication, does not validate link parameters or save requests, and accepts requests from any site, so anyone who can reach it can read and change its files. Do not run it on a public server, and disable it before going to production.

## Prerequisites

- **ONLYOFFICE Docs**: [self-hosted](https://www.onlyoffice.com/download?from=api#docs-developer) or [cloud](https://www.onlyoffice.com/docs-registration?from=api).
- **Ruby**: see the [official website](https://www.ruby-lang.org/en/downloads/).
- **GNU Make**: see the [official website](https://www.gnu.org/software/make/).
- **Git**: see the [official website](https://git-scm.com/downloads).

## Step 1. Download the example

Clone the [example repository](https://github.com/ONLYOFFICE/document-server-integration/tree/main/web/documentserver-example/ruby), go to the Ruby example, and fetch its submodules:

```sh
git clone --depth 1 https://github.com/ONLYOFFICE/document-server-integration
cd document-server-integration/web/documentserver-example/ruby
git submodule update --init --depth 1 .
```

## Step 2. Configure the connection

The example has no configuration file. It reads its settings from environment variables, so set them in the terminal where you will run the example in the next step:

```sh
export DOCUMENT_SERVER_PUBLIC_URL=http://documentserver
export STORAGE_PATH=storage
export JWT_SECRET=secret
export EXAMPLE_URL=http://example.com:3000
```

On Windows, use `set NAME=value` in Command Prompt or `$env:NAME = "value"` in PowerShell instead of `export`.

- `DOCUMENT_SERVER_PUBLIC_URL`: the address of the server with ONLYOFFICE Docs installed. Replace `documentserver` with its name or IP address.
- `STORAGE_PATH`: the folder where the example creates and stores files. The default is `storage` in the example folder. You can set an absolute path. The user running the example needs read and write permissions to this folder.
- `JWT_SECRET`: must match the JWT secret of ONLYOFFICE Docs. JWT is enabled in ONLYOFFICE Docs by default, but disabled in the example until this variable is set, so set it to the [secret key](https://api.onlyoffice.com/docs/docs-api/additional-api/signature/) of your server.
- `EXAMPLE_URL`: the address at which ONLYOFFICE Docs reaches the example. Set it if ONLYOFFICE Docs runs on another computer or in Docker, where the address you open in the browser, such as `localhost`, points somewhere else.

If you want to experiment with the editor configuration, modify the [parameters](https://api.onlyoffice.com/docs/docs-api/usage-api/advanced-parameters/) in the `config` method of the `app/models/file_model.rb` file.

## Step 3. Install the dependencies and run the example

```sh
make prod
make server-prod
```

Open `http://localhost:3000` in your browser. You will see the example's start page, where you can upload a file or create a new document, spreadsheet, presentation, or PDF form.

To run the example together with ONLYOFFICE Docs in [Docker](https://docs.docker.com/get-started/get-docker/) instead, run `make compose-prod`. It starts ONLYOFFICE Docs, the example, and a proxy that already have a matching configuration, and the example opens at `http://localhost`. The environment variables for this setup are in the `compose-base.yml` file.

## Troubleshooting

- **The document security token is not correctly formed**: JWT is disabled in the example (`JWT_SECRET` is not set), or `JWT_SECRET` does not match the secret of ONLYOFFICE Docs.
- **Download failed**: ONLYOFFICE Docs cannot reach the example. Set `EXAMPLE_URL` to an address that ONLYOFFICE Docs can resolve.

For more information, see the [documentation](https://api.onlyoffice.com/docs/docs-api/samples/language-specific-examples/ruby-example/).
