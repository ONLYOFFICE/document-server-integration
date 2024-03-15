## Overview

This example will help you integrate ONLYOFFICE Docs into your web application on Node.js.

**Please note**: It is intended for testing purposes and demonstrating functionality of the editors. Do NOT use this integration example on your own server without proper code modifications! In case you enabled the test example, disable it before going for production.

## For Windows

### Step 1. Install ONLYOFFICE Docs

Download and install ONLYOFFICE Docs (packaged as Document Server).

See the detailed guide to learn how to [install Document Server for Windows](https://helpcenter.onlyoffice.com/installation/docs-developer-install-windows.aspx).

### Step 2. Download the Node.js code for the editors integration

Download the [Node.js example](https://api.onlyoffice.com/editors/demopreview) from our site.

To connect the editors to your website, specify the path to the editors installation and the path to the storage folder in the *config/default.json* file:

```
"storageFolder": "./files"
"storagePath": "/files"
"siteUrl": "https://documentserver/"
```

where the **documentserver** is the name of the server with the ONLYOFFICE Document Server installed, the **storageFolder** and **storagePath** are the paths where files will be created and stored. You can set an absolute path. For example, *D:\\\\folder*. Please note that on Windows OS the double backslash must be used as a separator.

If you want to experiment with the editor configuration, modify the [parameters](https://api.onlyoffice.com/editors/advanced) in the *\views\editor.ejs* file.

### Step 3. Install Node.js environment

Install the **node.js** environment which is going to be used to run the Node.js project. Please follow the link at the [official website](https://nodejs.org/en/download/) choosing the correct version for your Windows OS (32-bit or 64-bit).

### Step 4. Run the Node.js code

We will run the code in Node.js runtime environment and will interact with it using the **command line interface (cmd)**.

1. Launch the **Command Prompt** and switch to the folder with the Node.js project code, for example:

    ```
    cd /d "C:\Node.js Example"
    ```

2. Node.js comes with a package manager, **node package manager (npm)**, which is automatically installed along with Node.js. To run the Node.js code, install the project modules using the following *npm* command:

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

### Step 5. Check accessibility

In case the example and Document Server are installed on different computers, make sure that your server with the example installed has access to the Document Server with the address which you specify instead of **documentserver** in the configuration files. 

Make sure that the Document Server has access to the server with the example installed with the address which you specify instead of **example.com** in the configuration files.

## For Linux

### Step 1. Install ONLYOFFICE Docs

Download and install ONLYOFFICE Docs (packaged as Document Server).

See the detailed guide to learn how to [install Document Server for Linux](https://helpcenter.onlyoffice.com/installation/docs-developer-install-ubuntu.aspx).

### Step 2. Install the prerequisites and run the website with the editors

1. Install **Node.js**:

    ```
    curl -sL https://deb.nodesource.com/setup_14.x | sudo -E bash -
    ```

    ```
    sudo apt-get install -y nodejs
    ```

2. Download the archive with the Node.js example and unpack it:

    ```
    wget https://api.onlyoffice.com/app_data/editor/Node.js.Example.zip
    ```

    ```
    unzip Node.js.Example.zip
    ```

3. Change the current directory for the project directory:

    ```
    cd Node.js\ Example/
    ```

4. Install the dependencies:

    ```
    npm install
    ```

5. Edit the *config/default.json* configuration file. Specify the name of your local server with the ONLYOFFICE Document Server installed.

    ```
    nano config/default.json
    ```

	Edit the following lines:

    ```
    "storageFolder": "./files"
    "storagePath": "/files"
    "siteUrl": "https://documentserver/"
    ```

	where the **documentserver** is the name of the server with the ONLYOFFICE Document Server installed, the **storageFolder** and **storagePath** are the paths where files will be created and stored. Please note that you must have read and write permissions to the folder. If you do not have them, please use the next command:
   ```
   sudo chmod -R ugo+rw /{path}
   ```

6. Run the project with Node.js:

    ```
    nodejs bin/www
    ```

7. See the result in your browser using the address:

    ```
    http://localhost:3000
    ```

### Step 3. Check accessibility

In case the example and Document Server are installed on different computers, make sure that your server with the example installed has access to the Document Server with the address which you specify instead of **documentserver** in the configuration files. 

Make sure that the Document Server has access to the server with the example installed with the address which you specify instead of **example.com** in the configuration files.

## Important security info

Please keep in mind the following security aspects when you are using test examples:

* There is no protection of the storage from unauthorized access since there is no need for authorization.
* There are no checks against parameter substitution in links, since the parameters are generated by the code according to the pre-arranged scripts.
* There are no data checks in requests of saving the file after editing, since each test example is intended for requests only from ONLYOFFICE Document Server.
* There are no prohibitions on using test examples from other sites, since they are intended to interact with ONLYOFFICE Document Server from another domain.