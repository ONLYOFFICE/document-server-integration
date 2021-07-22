## Overview

This example will help you integrate ONLYOFFICE Docs into your web application written in .Net Core.

It is aimed at testing the editors. Please, do not use it for production without proper modifications.

## Step 1. Install ONLYOFFICE Docs

Download and install ONLYOFFICE Docs (packaged as Document Server). 

See the detailed guide to learn how to install Document Server [for Windows](https://helpcenter.onlyoffice.com/installation/docs-developer-install-windows.aspx), [for Linux](https://helpcenter.onlyoffice.com/installation/docs-developer-install-ubuntu.aspx), or [for Docker](https://helpcenter.onlyoffice.com/server/developer-edition/docker/docker-installation.aspx).

## Step 2. Download the .Net Core code for the editors integration and change base configuration

Download the [.Net Core example](https://api.onlyoffice.com/editors/demopreview) from our site.
You need to connnect the editors to your web site. Specify path to the editors installation in the *appsettings.json* file:
```
"files.docservice.url.site": "https://documentserver/"
```
where the **documentserver** is the name of the server with the ONLYOFFICE Document Server installed.
If you want to experiment with the editor configuration, modify the [parameters](https://api.onlyoffice.com/editors/advanced) it the *FileModel.cs* file.

## Step 3. Install the prerequisites
Сheck if your system meets the system requirements:
* Microsoft .NET Core: version 3.1 (download it from the [official Microsoft website](https://dotnet.microsoft.com/download/dotnet/3.1));

You can install the prerequisites using scripts:
### For Windows
(https://docs.microsoft.com/en-us/dotnet/core/install/windows?tabs=net50#install-with-powershell-automation)

### For Linux
(https://docs.microsoft.com/en-us/dotnet/core/install/linux-scripted-manual#scripted-install)

## Step 4. Run your website with the editors
1. The example listenning on http://localhost:8000, but if you want change this value, you need go to folder **Propeties** and edit path in the *launchSettings.json* file:
```
"applicationUrl": "http://localhost:8000",
```
where the **http://localhost:8000** is the name of you listenning address.
2. From the current folder where the example is located in your command prompt, run the following command:
	> dotnet run
The **dotnet run** command will build and start the app. You can stop the app at any time by selecting **Ctrl+C**.
If you rebuild and restart the app whenever you make code changes, run the following command:
	> dotnet watch run
You can stop the app at any time by selecting **Ctrl+C**.

## Step 5. Check accessibility
In case the example and Document Server are installed on different computers, make sure that your server with the example installed has access to the Document Server with the address which you specify instead of **documentserver** in the configuration files. 

Make sure that the Document Server has access to the server with the example installed with the address which you specify instead of **example.com** in the configuration files.
