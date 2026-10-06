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
export JWT_SECRET=developer-only-not-a-real-secret
export EXAMPLE_URL=http://example.com:8000
```

On Windows, use `set NAME=value` in Command Prompt or `$env:NAME = "value"` in PowerShell instead of `export`.

- `DOCUMENT_SERVER_PUBLIC_URL`: the address of the server with ONLYOFFICE Docs installed. Replace `documentserver` with its name or IP address. If the example has to reach ONLYOFFICE Docs at a different address than the browser does, also set `DOCUMENT_SERVER_PRIVATE_URL` to that address.
- `STORAGE_PATH`: the folder where the example creates and stores files. The default is the `storage` folder inside the example directory. You can set an absolute path. The user running the example needs read and write permissions to this folder.
- `JWT_SECRET`: must match the JWT settings of ONLYOFFICE Docs. The default is `developer-only-not-a-real-secret`, a test value for development only. Set it to an empty value to disable JWT. Replace it with the [secret key](https://api.onlyoffice.com/docs/docs-api/additional-api/signature/) of your server.
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

## File API methods used in this example

The methods described below are available for Python test example.

### POST `/upload`

|                        |                                                              |
| ---------------------- | ------------------------------------------------------------ |
| **Summary**            | Upload file to test example via request                      |
| **URL**                | /upload                                                      |
| **Method**             | POST                                                         |
| **Request<br>Headers** | `Content-Type: multipart/form-data`                          |
| **Request<br>Body**    | `uploadedFile=@<filepath>`<br> `filepath` - file for uploading<br />Multipart body with the file binary contents |
| **Response**           | **Code:** 200 OK <br />**Content on success:**<br /> `{ "filename": <filename>}`<br />**Content on error:**<br /> `{ "error": "File type is not supported" }` <br /> Or <br /> `{ "error": "File size is incorrect" }` |
| **Sample**             | `curl -X POST -F uploadedFile=@filename.docx http://localhost/upload` |

### GET `/remove`

|                    |                                                              |
| ------------------ | ------------------------------------------------------------ |
| **Summary**        | Delete one file                                              |
| **URL**            | /remove                                                      |
| **Method**         | GET                                                          |
| ****URL Params**** | `fileName=[string]` - file for deleting. |
| **Response**       | **Code:** 200 OK <br /> **Success:**<br /> `{ "success": true }` |
| **Sample**         | `curl -X GET http://localhost/remove?fileName=filename.docx` |

### DELETE `/remove`

|                    |                                                              |
| ------------------ | ------------------------------------------------------------ |
| **Summary**        | Delete all files                                             |
| **URL**            | /remove                                                      |
| **Method**         | DELETE                                                       |
| **Response**       | **Code:** 200 OK <br /> **Success:**<br /> `{ "success": true }` |
| **Sample**         | `curl -X DELETE http://localhost/remove`                     |

### GET `/files`

|                    |                                                              |
| ------------------ | ------------------------------------------------------------ |
| **Summary**        | Get information about all files                              |
| **URL**            | /files                                                       |
| **Method**         | GET                                                          |
| **Response**       | **Code:** 200 OK <br /> **Success:**<br /> `[{ "version": <file_version>, "id": <file_id>, "contentLength": <file_size_in_kilobytes>, "pureContentLength": <file_size_in_bytes>, "title": <file_name>, "updated": <last_change_date>}, ..., {...}]` |
| **Sample**         | `curl -X GET http://localhost/files`                         |

## Troubleshooting

- **The document security token is not correctly formed**: JWT is disabled in the example (`JWT_SECRET` is set to an empty value), or `JWT_SECRET` does not match the secret of ONLYOFFICE Docs. It is set to the test value by default, so replace it with your server's secret and restart the example.
- **Download failed**: ONLYOFFICE Docs cannot reach the example. Set `EXAMPLE_URL` to an address that ONLYOFFICE Docs can resolve, and restart the example.

For more information, see the [documentation](https://api.onlyoffice.com/docs/docs-api/samples/language-specific-examples/python-example/).
