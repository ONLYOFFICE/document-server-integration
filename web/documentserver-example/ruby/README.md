## Overview

This example will help you integrate ONLYOFFICE Docs into your web application written on Ruby.

It is aimed at testing the editors. Please, do not use it for production without proper modifications. 

## Step 1. Install ONLYOFFICE Docs

Download and install ONLYOFFICE Docs (packaged as Document Server).

See the detailed guide to learn how to install Document Server [for Windows](https://helpcenter.onlyoffice.com/installation/docs-developer-install-windows.aspx), [for Linux](https://helpcenter.onlyoffice.com/installation/docs-developer-install-ubuntu.aspx), or [for Docker](https://helpcenter.onlyoffice.com/server/developer-edition/docker/docker-installation.aspx).

## Step 2. Install the prerequisites and run the website with the editors

1. Install **Ruby Version Manager (RVM)** and the latest stable **Ruby** version:

    ```
    gpg --keyserver "hkp://keys.gnupg.net" --recv-keys 409B6B1796C275462A1703113804BB82D39DC0E3
    ```

    ```
    \curl -sSL https://get.rvm.io | bash -s stable --ruby
    ```

2. Download the archive with the Ruby example and unpack the archive:

    ```
    wget "https://api.onlyoffice.com/app_data/editor/Ruby%20Example.zip"
    ```

    ```
    unzip Ruby\ Example.zip
    ```

3. Change the current directory for the project directory:

    ```
    cd Ruby\ Example
    ```

4. Install the dependencies:

    ```
    bundle install
    ```

5. Edit the *application.rb* configuration file. Specify the name of your local server with the ONLYOFFICE Document Server installed.

    ```
    nano config/application.rb
    ```

	Edit the following lines:

    ```
    Rails.configuration.urlSite="https://documentserver/"
    ```

	Where the **documentserver** is the name of the server with the ONLYOFFICE Document Server installed.

6. Run the **Rails** application:

    ```
    rails s -b 0.0.0.0 -p 80
    ```

7. See the result in your browser using the address:

    ```
    http://localhost
    ```

	If you want to experiment with the editor configuration, modify the [parameters](https://api.onlyoffice.com/editors/advanced) in the *views\home\editor.html.erb* file.

## Step 3. Check accessibility

In case the example and Document Server are installed on different computers, make sure that your server with the example installed has access to the Document Server with the address which you specify instead of **documentserver** in the configuration files. 

Make sure that the Document Server has access to the server with the example installed with the address which you specify instead of **example.com** in the configuration files.
