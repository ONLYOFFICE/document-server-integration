## Document Server integration example

These examples show the way to integrate [ONLYOFFICE Document Server][2] into your own website or application using one of the programming languages. The package contains examples written in .Net (C# MVC), .Net (C#), Java, Node.js, PHP and Ruby.

You should change `http://documentserver` to your server address in these files:
* .Net (C# MVC) - `web/documentserver-example/csharp-mvc/web.appsettings.config`
* .Net (C#) - `web/documentserver-example/csharp/settings.config`
* Java - `web/documentserver-example/java/src/main/resources/settings.properties`
* Node.js - `web/documentserver-example/nodejs/config/default.json`
* PHP - `web/documentserver-example/php/config.php`
* Ruby - `web/documentserver-example/ruby/config/application.rb`

More information on how to use these examples can be found here: [http://api.onlyoffice.com/editors/demopreview](http://api.onlyoffice.com/editors/demopreview "http://api.onlyoffice.com/editors/demopreview")

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

If you have any problems with or questions about [ONLYOFFICE Document Server][2], please visit our official forum to find answers to your questions: [dev.onlyoffice.org][1] or you can ask and answer ONLYOFFICE development questions on [Stack Overflow][3].

  [1]: http://dev.onlyoffice.org
  [2]: https://github.com/ONLYOFFICE/DocumentServer
  [3]: http://stackoverflow.com/questions/tagged/onlyoffice
  
## License

document-server-integration is released under the MIT License. See the LICENSE.txt file for more information.
