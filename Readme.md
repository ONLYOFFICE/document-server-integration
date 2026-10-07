## Integration examples

The integration examples are small web applications that list files, open them in [ONLYOFFICE Docs][2], and save them back through the callback handler. Each one does the same job in a different language or framework. Run the one closest to your stack to see a complete integration working before you write your own.

> [!WARNING]
> The examples are for testing only. They have no authentication, do not validate link parameters or save requests, and accept requests from any site, so anyone who can reach them can read and change their files. Do not run them on a public server, and disable them before going to production.

| Example | Built with | Settings | Runs with |
| ------- | ---------- | -------- | --------- |
| [.NET (C#)](web/documentserver-example/csharp) | ASP.NET Web Forms | `settings.config` | Visual Studio and IIS, Windows only |
| [.NET (C# MVC)](web/documentserver-example/csharp-mvc) | ASP.NET MVC | `web.appsettings.config` | Visual Studio and IIS, Windows only |
| [Go](web/documentserver-example/go) | gorilla/mux | `config/configuration.json` | `go run` |
| [Java](web/documentserver-example/java) | Servlets and JSP | `src/main/resources/settings.properties` | Docker Compose |
| [Java Spring](web/documentserver-example/java-spring) | Spring Boot | `src/main/resources/application.properties` | Maven |
| [Node.js](web/documentserver-example/nodejs) | Express | `config/default.json` | npm |
| [PHP](web/documentserver-example/php) | No framework | Environment variables | PHP built-in server or Docker Compose |
| [PHP Laravel](web/documentserver-example/php-laravel) | Laravel | `.env` | Docker Compose |
| [Python](web/documentserver-example/python) | Django | Environment variables | Django server or Docker Compose |
| [Ruby](web/documentserver-example/ruby) | Ruby on Rails | Environment variables | Make |

Whichever example you choose, you set the same three things:

- **The address of ONLYOFFICE Docs**, from which the browser loads the editors and to which the example sends its requests.
- **The JWT secret**, which must match the [secret key](https://api.onlyoffice.com/docs/docs-api/additional-api/signature/) of ONLYOFFICE Docs.
- **The address of the example**, at which ONLYOFFICE Docs downloads files and sends the callback. Set it when ONLYOFFICE Docs runs on another computer or in Docker, where `localhost` points somewhere else.

The README of each example shows where these settings are and what they are called. For more information, see the [documentation](https://api.onlyoffice.com/docs/docs-api/samples/language-specific-examples/).

## API methods for test examples

The methods described below are available for all of the test examples.

### POST `/upload`

|                        |                                                              |
| ---------------------- | ------------------------------------------------------------ |
| **Summary**            | Upload file to test example via request                      |
| **URL**                | /upload                                                      |
| **Method**             | POST                                                         |
| **Request<br>Headers** | `Content-Type: multipart/form-data`                          |
| **Request<br>Body**    | `uploadedFile=@<filepath>`<br> `filepath` - file for uploading<br />Multipart body with the file binary contents |
| **Response**           | **Code:** 200 OK <br />**Content on success:**<br /> `{ "filename": <filename>}`<br />**Content on error:**<br /> `{ "error": "Uploaded file not found" }` <br /> Or <br /> `{ "error": "File size is incorrect" }` |
| **Sample**             | `curl -X POST -F uploadedFile=@filename.docx http://localhost/upload` |


### DELETE `/file`

|                    |                                                              |
| ------------------ | ------------------------------------------------------------ |
| **Summary**        | Delete one file or all files                                 |
| **URL**            | /file                                                        |
| **Method**         | DELETE                                                       |
| ****URL Params**** | **Optional:**<br /> `filename=[string]` - file for deleting. <br /> *WARNING! Without this parameter, all files will be deleted* |
| **Response**       | **Code:** 200 OK <br /> **Success:**<br /> `{ "success": true }` |
| **Sample**         | **Delete one file:**<br />`curl -X DELETE http://localhost/file?filename=filename.docx`<br />**Delete all files:**<br />`curl -X DELETE http://localhost/file`<br /> |


### GET `/files`

|                    |                                                              |
| ------------------ | ------------------------------------------------------------ |
| **Summary**        | Get information about all files                              |
| **URL**            | /files                                                       |
| **Method**         | GET                                                          |
| **Response**       | **Code:** 200 OK <br /> **Success:**<br /> `[{ "version": <file_version>, "id": <file_id>, "contentLength": <file_size_in_kilobytes>, "pureContentLength": <file_size_in_bytes>, "title": <file_name>, "updated": <last_change_date>}, ..., {...}]` |
| **Sample**         | `curl -X GET http://localhost/files/`                        |


### GET `/files/file/{fileId}`

|                    |                                                              |
| ------------------ | ------------------------------------------------------------ |
| **Summary**        | Get information about a file by file id                      |
| **URL**            | /files/file/{fileId}                                         |
| **Method**         | GET                                                          |
| **Response**       | **Code:** 200 OK <br />**Content on success:**<br /> `[{ "version": <file_version>, "id": <file_id>, "contentLength": <file_size_in_kilobytes>, "pureContentLength": <file_size_in_bytes>, "title": <file_name>, "updated": <last_change_date>}]`<br />**Content on error:**<br /> `"File not found"` |
| **Sample**         | `curl -X GET http://localhost/files/{fileId}`                |

## Project Information

Official website: [https://www.onlyoffice.com](https://www.onlyoffice.com/?utm_source=github&utm_medium=cpc&utm_campaign=GitHubIntegrationEx)

Code repository: [https://github.com/ONLYOFFICE/document-server-integration](https://github.com/ONLYOFFICE/document-server-integration "https://github.com/ONLYOFFICE/document-server-integration")

ONLYOFFICE for developers: [https://www.onlyoffice.com/developer-edition.aspx](https://www.onlyoffice.com/developer-edition.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubIntegrationEx)

## User Feedback and Support

If you have any problems with or questions about [ONLYOFFICE Document Server][2], please visit our official forum to find answers to your questions: [forum.onlyoffice.com][1] or you can ask and answer ONLYOFFICE development questions on [Stack Overflow][3].

  [1]: https://forum.onlyoffice.com
  [2]: https://github.com/ONLYOFFICE/DocumentServer
  [3]: http://stackoverflow.com/questions/tagged/onlyoffice
  
## License

document-server-integration is released under the Apache-2.0 License. See the LICENSE file for more information.
