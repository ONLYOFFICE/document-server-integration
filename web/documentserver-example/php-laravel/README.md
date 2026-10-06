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
DOCUMENT_SERVER_JWT_SECRET=developer-only-not-a-real-secret
```

- `DOCUMENT_SERVER_PUBLIC_URL` and `DOCUMENT_SERVER_PRIVATE_URL`: the address of your ONLYOFFICE Docs server.
- `DOCUMENT_STORAGE_PRIVATE_URL`: the address at which ONLYOFFICE Docs reaches the example. The name `proxy` resolves only inside the Docker network.
- `DOCUMENT_SERVER_JWT_SECRET`: the [secret key](https://api.onlyoffice.com/docs/docs-api/additional-api/signature/) of your server. The default is `developer-only-not-a-real-secret`, a test value for development only.

If you want to experiment with the editor configuration, modify the [parameters](https://api.onlyoffice.com/docs/docs-api/usage-api/advanced-parameters/) in the `app/Models/Editor/Editor.php` file.

## Step 3. Build and run the example

```sh
docker compose up -d --build
```

Open `http://localhost` in your browser. You will see the example's start page, where you can upload a file or create a new document, spreadsheet, presentation, or PDF form.

The example's files, including `.env`, are copied into a Docker volume on the first start. After you change them, run `docker compose down -v` and start the example again.

## File API methods used in this example

The methods described below are available for PHP Laravel test example.

### POST `/upload`

|                        |                                                              |
| ---------------------- | ------------------------------------------------------------ |
| **Summary**            | Upload file to test example via request                      |
| **URL**                | /upload                                                      |
| **Method**             | POST                                                         |
| **URL Params**         | `user=[string]` - user id.                                   |
| **Request<br>Headers** | `Content-Type: multipart/form-data`                          |
| **Request<br>Body**    | `uploadedFile=@<filepath>`<br> `filepath` - file for uploading<br />Multipart body with the file binary contents |
| **Response**           | **Code:** 200 OK <br />**Content on success:**<br /> `{ "filename": <filename>}`<br />**Content on error:**<br /> `{ "error": "File type is not supported" }` <br /> Or <br /> `{ "error": "File size is incorrect" }` |
| **Sample**             | `curl -X POST -F uploadedFile=@filename.docx http://localhost/upload?user=uid-1` |

### DELETE `/files/delete`

|                    |                                                              |
| ------------------ | ------------------------------------------------------------ |
| **Summary**        | Delete one file or all files                                 |
| **URL**            | /files/delete                                                |
| **Method**         | DELETE                                                       |
| ****URL Params**** | **Optional:**<br /> `filename=[string]` - file for deleting. <br /> *WARNING! Without this parameter, all files will be deleted* |
| **Response**       | **Code:** 200 OK <br /> **Success:**<br /> `{ "success": true }` |
| **Sample**         | **Delete one file:**<br />`curl -X DELETE http://localhost/files/delete?filename=filename.docx`<br />**Delete all files:**<br />`curl -X DELETE http://localhost/files/delete`<br /> |

### GET `/files`

|                    |                                                              |
| ------------------ | ------------------------------------------------------------ |
| **Summary**        | Get information about all files                              |
| **URL**            | /files                                                       |
| **Method**         | GET                                                          |
| **Response**       | **Code:** 200 OK <br /> **Success:**<br /> `[{ "version": <file_version>, "id": <file_id>, "contentLength": <file_size_in_kilobytes>, "pureContentLength": <file_size_in_bytes>, "title": <file_name>, "updated": <last_change_date>}, ..., {...}]` |
| **Sample**         | `curl -X GET http://localhost/files`                         |

## Troubleshooting

- **The document security token is not correctly formed**: `DOCUMENT_SERVER_JWT_SECRET` does not match the secret of ONLYOFFICE Docs. If you changed it after the first start, recreate the volume with `docker compose down -v`.
- **Download failed**: ONLYOFFICE Docs cannot reach the example. Set `DOCUMENT_STORAGE_PRIVATE_URL` in `.env` to an address that ONLYOFFICE Docs can resolve.
- **Laravel installation or configuration problems**: see the [Laravel documentation](https://laravel.com/docs/11.x/deployment#server-configuration).

For more information, see the [documentation](https://api.onlyoffice.com/docs/docs-api/samples/language-specific-examples/php-laravel-example/).
