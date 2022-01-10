## Overview

This example will help you integrate ONLYOFFICE Docs into your web application written in Python.

It is aimed at testing the editors. Please, do not use it for production without proper modifications.

## Step 1. Install ONLYOFFICE Docs

Download and install ONLYOFFICE Docs (packaged as Document Server).

See the detailed guide to learn how to install Document Server [for Windows](https://helpcenter.onlyoffice.com/installation/docs-developer-install-windows.aspx?from=api_python_example), [for Linux](https://helpcenter.onlyoffice.com/installation/docs-developer-install-ubuntu.aspx?from=api_python_example), or [for Docker](https://helpcenter.onlyoffice.com/server/developer-edition/docker/docker-installation.aspx?from=api_python_example).

## Step 2. Install the prerequisites and run the website with the editors

1. **Python** comes preinstalled on most Linux distributions, and is available as a package on all others. Python 3.9 is required. Please proceed to [official documentation](https://docs.python.org/3/using/unix.html) if you have any troubles.

2. Download the archive with the Python example and unpack it:

    ```
    wget "https://api.onlyoffice.com/app_data/editor/Python%20Example.zip"
    ```

    ```
    unzip Python\ Example.zip
    ```

3. Change the current directory for the project directory:

    ```
    cd Python\ Example
    ```

4. Install the dependencies:

    ```
    pip install Django==3.1.3
    pip install requests==2.25.0
    pip install pyjwt==1.7.1
    pip install python-magic
    ```

5. Edit the *config.py* configuration file. Specify the name of your local server with the ONLYOFFICE Document Server installed. And specify the name of the server on which example is installed.

    ```
    nano config.py
    ```

	Edit the following lines:

    ```
	STORAGE_PATH = 'app_data'
    DOC_SERV_SITE_URL = 'https://documentserver/'
    ```

	where the **documentserver** is the name of the server with the ONLYOFFICE Document Server installed and the **STORAGE_PATH** is the path where files will be created and stored. You can set an absolute path. For example, *D:\\\\folder*. Please note that on Windows OS the double backslash must be used as a separator.

6. Run the **Python** server:

    ```
    python manage.py runserver 0.0.0.0:8000
    ```

7. See the result in your browser using the address:

    ```
    http://localhost:8000
    ```

## Step 3. Check accessibility

In case the example and Document Server are installed on different computers, make sure that your server with the example installed has access to the Document Server with the address which you specify instead of **documentserver** in the configuration files. 

Make sure that the Document Server has access to the server with the example installed with the address which you specify instead of **example.com** in the configuration files.
