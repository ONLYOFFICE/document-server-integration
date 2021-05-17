## Overview

This example will help you integrate ONLYOFFICE Docs into your web application on Node.js.

It is aimed at testing the editors. Please, do not use it for production without proper modifications. 

## For Windows

### Step 1. Install Document Server

Download and install ONLYOFFICE Docs (packaged as Document Server). 

See the detailed guide to learn how to [install Document Server for Windows](https://helpcenter.onlyoffice.com/installation/docs-developer-install-windows.aspx).

### Step 2. Download the Node.js code for the editors integration

Download the [Node.js example](https://api.onlyoffice.com/editors/demopreview) from our site.

You need to connect the editors to your website. Specify the path to the editors installation in the *config/default.json* file:

```
"siteUrl": "https://documentserver/"
```

where the **documentserver** is the name of the server with the ONLYOFFICE Document Server installed.

If you want to experiment with the editor configuration, modify the [parameters](https://api.onlyoffice.com/editors/advanced) it the *\views\editor.ejs* file.

### Step 3. Install Node.js environment

Install the **node.js** environment which is going to be used to run the Node.js project. Please follow the link at the official website: https://nodejs.org/en/download/ choosing the correct version for your Windows OS (32-bit or 64-bit).

### Step 4. Run the Node.js code

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

In case the example and Document Server are installed on different computers, make sure that your server with the example installed has access to the Document Server with the address which you specify instead of **documentserver** in the configuration files. 

Make sure that the Document Server has access to the server with the example installed with the address which you specify instead of **example.com** in the configuration files.

## For Linux

### Step 1. Install Document Server

Download and install ONLYOFFICE Docs (packaged as Document Server).

See the detailed guide to learn how to [install Document Server for Linux](https://helpcenter.onlyoffice.com/installation/docs-developer-install-ubuntu.aspx).

## Step 2. Install the prerequisites and run the website with the editors

1. Install **Node.js**:

    ```
    curl -sL https://deb.nodesource.com/setup_14.x | sudo -E bash -
    ```

    ```
    sudo apt-get install -y nodejs
    ```

2. Download the archive with the Node.js example and unpack it:

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

In case the example and Document Server are installed on different computers, make sure that your server with the example installed has access to the Document Server with the address which you specify instead of **documentserver** in the configuration files. 

Make sure that the Document Server has access to the server with the example installed with the address which you specify instead of **example.com** in the configuration files.
