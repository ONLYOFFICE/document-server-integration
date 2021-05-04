# How to integrate online editors into your own web site on Node.js

## Introduction

To integrate **ONLYOFFICE online editors** into your own website on **Node.js** you need to download and install ONLYOFFICE editors on your local server and use the [Node.js Example](https://api.onlyoffice.com/editors/demopreview) for their integration. We will show you how to run the Node.js example on [Windows OS](#running-the-example-on-windows-os) and [Linux OS](#running-the-example-on-linux-os).

Please nore that the integration examples are used to demonstrate document editors functions and the ways to connect **Document Server** to your own application. **DO NOT USE** these examples on your own server without **PROPER CODE MODIFICATIONS**!

This guide will show you the sequence of actions to integrate the editors successfully.

## Running the example on Windows OS

## Step 1. Download and Install Document Server

First, download the [**ONLYOFFICE Editors**](https://api.onlyoffice.com/editors/demopreview) (the ONLYOFFICE Document Server).

See the detailed guide to learn how to [install Document Server for Windows](https://helpcenter.onlyoffice.com/installation/docs-developer-install-windows.aspx?from=api_nodejs_example).

## Step 2. Download the Node.js code for the editors integration

Download the [Node.js example](https://api.onlyoffice.com/editors/demopreview) from our site.

You need to connect the editors to your web site. For that specify the path to the editors installation in the *config/default.json* file:

```
"siteUrl": "https://documentserver/"
```

where the **documentserver** is the name of the server with the ONLYOFFICE Document Server installed.

If you want to experiment with the editor configuration, modify the [parameters](https://api.onlyoffice.com/editors/advanced) it the *\views\editor.ejs* file.

## Step 3. System requirements

Download and install the **node.js** environment which is going to be used to run the Node.js project. Please follow the link at the official website: https://nodejs.org/en/download/, choosing the correct version for your Windows OS (32-bit or 64-bit).

## Step 4. Running the Node.js code

We will run the code in Node.js runtime environment and will interact with it using the command line interface (cmd).

1. Launch the **Command Prompt** and switch to the folder with the Node.js project code, for example:

    ```
    cd /d "C:\Node.js Example"
    ```

2. Node.js comes with a package manager, **node package manager (npm)**, which is automatically installed along with Node.js. To run the Node.js code, install the project modules using the following npm command:

    ```
    npm install
    ```

	A new *node_modules* folder will be created in the project folder.

3. Run the project using the **Command Prompt**:

    ```
    node bin/www
    ```

4. See the result in your browser using the address:

    ```
    http://localhost:3000
    ```

## Step 5. Checking accessibility

In case the example and Document Server are installed on different computers, make sure that your server with the example installed has access to the Document Server with the address which you specify instead of **documentserver** in the configuration files. And you must also make sure that the Document Server in its turn has access to the server with the example installed with the address which you specify instead of **example.com** in the configuration files.

## Running the example on Linux OS

## Step 1. Download and Install Document Server

First, download the [**ONLYOFFICE Editors**](https://api.onlyoffice.com/editors/demopreview) (the ONLYOFFICE Document Server).

See the detailed guide to learn how to [install Document Server for Linux](https://helpcenter.onlyoffice.com/installation/docs-developer-install-ubuntu.aspx?from=api_nodejs_example).

## Step 2. Install the prerequisites and run the web site with the editors

1. Install **Node.js**:

    ```
    curl -sL https://deb.nodesource.com/setup_14.x | sudo -E bash -
    ```

    ```
    sudo apt-get install -y nodejs
    ```

2. Download the archive with the Node.js example and unpack the archive:

    ```
    wget https://api.onlyoffice.com/app_data/editor/Node.js%20Example.zip
    ```

    ```
    unzip Node.js\ Example.zip
    ```

3. Change the current directory for the project directory:

    ```
    cd Node.js\ Example/
    ```

4. Install the dependencies:

    ```
    npm install
    ```

5. Edit the *default.json* configuration file. Specify the name of your local server with the ONLYOFFICE Document Server installed.

    ```
    nano config/default.json
    ```

	Edit the following lines:

    ```
    "siteUrl": "https://documentserver/"
    ```

	Where the **documentserver** is the name of the server with the ONLYOFFICE Document Server installed.

6. Run the project with Node.js:

    ```
    nodejs bin/www
    ```

7. See the result in your browser using the address:

    ```
    http://localhost:3000
    ```

## Step 3. Checking accessibility

In case the example and Document Server are installed on different computers, make sure that your server with the example installed has access to the Document Server with the address which you specify instead of **documentserver** in the configuration files. And you must also make sure that the Document Server in its turn has access to the server with the example installed with the address which you specify instead of **example.com** in the configuration files.