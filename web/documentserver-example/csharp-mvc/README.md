# .NET (C# MVC) integration

The .NET (C# MVC) integration example is a small ASP.NET MVC application for .NET Framework 4.8 that lists files, opens them in ONLYOFFICE Docs, and saves them back through the callback handler. Run it to see a complete integration working before you write your own. The example runs on Windows only, under IIS. For the Web Forms version, see the [.NET (C#) example](https://github.com/ONLYOFFICE/document-server-integration/tree/main/web/documentserver-example/csharp).

> [!WARNING]
> This example is for testing only. It has no authentication, does not validate link parameters or save requests, and accepts requests from any site, so anyone who can reach it can read and change its files. Do not run it on a public server, and disable it before going to production.

## Prerequisites

- **ONLYOFFICE Docs**: [self-hosted](https://www.onlyoffice.com/download?from=api#docs-developer) or [cloud](https://www.onlyoffice.com/docs-registration?from=api).
- **.NET Framework 4.8**: see the [official website](https://dotnet.microsoft.com/download/dotnet-framework/net48).
- **Internet Information Services (IIS)**: see the [official website](https://learn.microsoft.com/iis/get-started/whats-new-in-iis-10/installing-iis-10).
- **Visual Studio**: see the [official website](https://visualstudio.microsoft.com/downloads/).
- **Git**: see the [official website](https://git-scm.com/downloads).

When you turn on IIS in **Windows Features**, open **Internet Information Services** > **World Wide Web Services** and select **.NET Extensibility 4.8**, **ASP.NET 4.8**, **ISAPI Extensions**, **ISAPI Filters**, **Default Document**, and **Request Filtering**. In Visual Studio, install the **ASP.NET and web development** workload.

## Step 1. Download the example

Clone the [example repository](https://github.com/ONLYOFFICE/document-server-integration/tree/main/web/documentserver-example/csharp-mvc), go to the .NET (C# MVC) example, and fetch its submodules:

```sh
git clone --depth 1 https://github.com/ONLYOFFICE/document-server-integration
cd document-server-integration/web/documentserver-example/csharp-mvc
git submodule update --init --depth 1 .
```

## Step 2. Configure the connection

Open the `web.appsettings.config` file and edit the following keys:

```xml
<appSettings>
  <add key="files.docservice.url.site" value="http://documentserver/"/>
  <add key="storage-path" value=""/>
  <add key="files.docservice.secret" value="" />
  <add key="files.docservice.url.example" value=""/>
</appSettings>
```

- `files.docservice.url.site`: the address of the server with ONLYOFFICE Docs installed. Replace `documentserver` with its name or IP address, and keep the trailing slash.
- `storage-path`: the folder where the example creates and stores files. If it is empty, the example stores files in its own folder, in a subfolder named after the user's IP address. You can set an absolute path, for example, `D:\folder`. The IIS application pool identity needs read and write permissions to this folder.
- `files.docservice.secret`: must match the JWT settings of ONLYOFFICE Docs. The key is empty by default, which disables JWT in the example. JWT is enabled in ONLYOFFICE Docs by default, so set this key to the [secret key](https://api.onlyoffice.com/docs/docs-api/additional-api/signature/) of your server.
- `files.docservice.url.example`: the address at which ONLYOFFICE Docs reaches the example. Set it if ONLYOFFICE Docs runs on another computer or in Docker, where the address you open in the browser, such as `localhost`, points somewhere else.

If you want to experiment with the editor configuration, modify the [parameters](https://api.onlyoffice.com/docs/docs-api/usage-api/advanced-parameters/) in the `Models/FileModel.cs` file.

## Step 3. Build and run the example

1. Open `OnlineEditorsExampleMVC.sln` in Visual Studio and select **Build** > **Build Solution**. Visual Studio restores the NuGet packages and compiles the example into the `bin` folder.
2. In IIS Manager, right-click **Sites** and select **Add Website**. Set **Physical path** to the example folder and **Port** to any free port.
3. In **Application Pools**, make sure that the pool of the new site uses **.NET CLR version** `v4.0`.
4. Right-click the site and select **Manage Website** > **Browse**.

The browser opens `http://localhost:<port>/`. You will see the example's start page, where you can upload a file or create a new document, spreadsheet, presentation, or PDF form.

## Troubleshooting

- **The document security token is not correctly formed**: JWT is disabled in the example (`files.docservice.secret` is empty), or `files.docservice.secret` does not match the secret of ONLYOFFICE Docs.
- **Download failed**: ONLYOFFICE Docs cannot reach the example. Set `files.docservice.url.example` in `web.appsettings.config` to an address that ONLYOFFICE Docs can resolve.

For more information, see the [documentation](https://api.onlyoffice.com/docs/docs-api/samples/language-specific-examples/net-example/).
