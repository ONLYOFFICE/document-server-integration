# Java integration

The Java integration example is a small servlet and JSP application for Apache Tomcat that lists files, opens them in ONLYOFFICE Docs, and saves them back through the callback handler. Run it to see a complete integration working before you write your own.

> [!WARNING]
> This example is for testing only. It has no authentication, does not validate link parameters or save requests, and accepts requests from any site, so anyone who can reach it can read and change its files. Do not run it on a public server, and disable it before going to production.

## Prerequisites

- **ONLYOFFICE Docs**: [self-hosted](https://www.onlyoffice.com/download?from=api#docs-developer) or [cloud](https://www.onlyoffice.com/docs-registration?from=api).
- **Docker**: see the [official website](https://docs.docker.com/get-started/get-docker/).
- **Git**: see the [official website](https://git-scm.com/downloads).

Docker Compose builds the example with Maven and JDK 8 and runs it in Apache Tomcat, so you do not need to install Java, Maven, or Tomcat yourself.

## Step 1. Download the example

Clone the [example repository](https://github.com/ONLYOFFICE/document-server-integration/tree/main/web/documentserver-example/java), go to the Java example, and fetch its submodules:

```sh
git clone --depth 1 https://github.com/ONLYOFFICE/document-server-integration
cd document-server-integration/web/documentserver-example/java
git submodule update --init --depth 1 .
```

## Step 2. Configure the connection

Open the `src/main/resources/settings.properties` file and edit the following keys:

```ini
storage-folder=app_data
files.docservice.url.site=http://documentserver/
files.docservice.url.example=
files.docservice.secret=developer-only-not-a-real-secret
```

- `files.docservice.url.site`: the address of the server with ONLYOFFICE Docs installed. Replace `documentserver` with its name or IP address and keep the trailing slash. The example runs in a Docker container, so do not use `localhost`: the address must be reachable both from your browser and from the container.
- `storage-folder`: the folder where the example creates and stores files. A relative path is resolved inside the deployed application, so with Docker the files are kept in the container. You can set an absolute path. In a `.properties` file, escape each backslash, for example, `D:\\folder`. The user running the example needs read and write permissions to this folder.
- `files.docservice.secret`: must match the JWT secret of ONLYOFFICE Docs. The default is `developer-only-not-a-real-secret`, a test value for development only. Replace it with the [secret key](https://api.onlyoffice.com/docs/docs-api/additional-api/signature/) of your server.
- `files.docservice.url.example`: the address at which ONLYOFFICE Docs reaches the example. Set it if ONLYOFFICE Docs runs on another computer or in Docker, where the address you open in the browser, such as `localhost`, points somewhere else.

The settings file is packaged into the application when it is built, so rebuild the example after each change.

If you want to experiment with the editor configuration, modify the [parameters](https://api.onlyoffice.com/docs/docs-api/usage-api/advanced-parameters/) in the `src/main/java/entities/FileModel.java` file. This class builds the configuration that `src/main/webapp/editor.jsp` passes to the editor.

## Step 3. Build and run the example

```sh
docker compose up --build
```

Open `http://localhost:8080` in your browser. You will see the example's start page, where you can upload a file or create a new document, spreadsheet, presentation, or PDF form.

## File API methods used in this example

The methods described below are available for Java test example.

### POST `/IndexServlet?type=upload`

|                        |                                                              |
| ---------------------- | ------------------------------------------------------------ |
| **Summary**            | Upload file to test example via request                      |
| **URL**                | /IndexServlet?type=upload                                    |
| **Method**             | POST                                                         |
| **Request<br>Headers** | `Content-Type: multipart/form-data`                          |
| **Request<br>Body**    | `uploadedFile=@<filepath>`<br> `filepath` - file for uploading<br />Multipart body with the file binary contents |
| **Response**           | **Code:** 200 OK <br />**Content on success:**<br /> `{ "filename": <filename>}`<br />**Content on error:**<br /> `{ "error": "File type is not supported" }` <br /> Or <br /> `{ "error": "File size is incorrect" }` |
| **Sample**             | `curl -X POST -F uploadedFile=@filename.docx http://localhost/IndexServlet?type=upload` |

### GET `/IndexServlet?type=remove`

|                    |                                                              |
| ------------------ | ------------------------------------------------------------ |
| **Summary**        | Delete one file or all files                                 |
| **URL**            | /IndexServlet?type=remove                                    |
| **Method**         | GET                                                          |
| ****URL Params**** | **Optional:**<br /> `filename=[string]` - file for deleting. <br /> *WARNING! Without this parameter, all files will be deleted* |
| **Response**       | **Code:** 200 OK <br /> **Success:**<br /> `{ "success": true }` |
| **Sample**         | **Delete one file:**<br />`curl -X GET http://localhost/IndexServlet?type=remove&filename=filename.docx`<br />**Delete all files:**<br />`curl -X GET http://localhost/IndexServlet?type=remove`<br /> |

### GET `/IndexServlet?type=files`

|                    |                                                              |
| ------------------ | ------------------------------------------------------------ |
| **Summary**        | Get information about all files                              |
| **URL**            | /IndexServlet?type=files                                     |
| **Method**         | GET                                                          |
| **Response**       | **Code:** 200 OK <br /> **Success:**<br /> `[{ "version": <file_version>, "id": <file_id>, "contentLength": <file_size_in_kilobytes>, "pureContentLength": <file_size_in_bytes>, "title": <file_name>, "updated": <last_change_date>}, ..., {...}]` |
| **Sample**         | `curl -X GET http://localhost/IndexServlet?type=files`       |

## Troubleshooting

- **The document security token is not correctly formed**: `files.docservice.secret` does not match the secret of ONLYOFFICE Docs. It is set to the test value by default, so replace it with your server's secret and rebuild the example.
- **Download failed**: ONLYOFFICE Docs cannot reach the example. Set `files.docservice.url.example` in `src/main/resources/settings.properties` to an address that ONLYOFFICE Docs can resolve, and rebuild the example.

For more information, see the [documentation](https://api.onlyoffice.com/docs/docs-api/samples/language-specific-examples/java-example/).
