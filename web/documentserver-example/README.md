# Integration examples

## To start integrating document editors into your own website you need to do the following:

1. Download [Document Server installation](https://www.onlyoffice.com/developer-edition-request.aspx?from=api.onlyoffice.com) and set it up on your local server.

2. Select the programming language and clone the source code for the sample of online editors integration into your web site.

3. [Edit the configuration files](https://api.onlyoffice.com/editors/advanced) in the sample changing the default path for the one to the editors installed at step 1 and other advanced parameters available for editor configuration.

4. In case the example and Document Server are installed on different computers, make sure that your server with the example installed has access to the Document Server with the address which you specify instead of **documentserver** in the configuration files. And you must also make sure that the Document Server in its turn has access to the server with the example installed with the address which you specify instead of **example.com** in the configuration files.

Please note that the integration examples are used to demonstrate document editors functions and the ways to connect **Document Server** to your own application. **DO NOT USE** these examples on your own server without **PROPER CODE MODIFICATIONS**!

The result should look like the [demo preview](https://api.onlyoffice.com/editors/demopreview#DemoPreview) on our web site.

If you have any further questions, please contact us at [integration@onlyoffice.com](mailto:integration@onlyoffice.com).