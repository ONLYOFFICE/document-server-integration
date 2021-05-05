# How to integrate online editors into your own web site on Ruby

## Introduction

To integrate **ONLYOFFICE online editors** into your own website on **Ruby** you need to download and install ONLYOFFICE editors on your local server and use the [Ruby Example](https://api.onlyoffice.com/editors/demopreview) for their integration. We will show how to run the Ruby example on Linux OS.

Please note that the integration examples are used to demonstrate document editors functions and the ways to connect **Document Server** to your own application. **DO NOT USE** these examples on your own server without **PROPER CODE MODIFICATIONS**!

This guide will show you the sequence of actions to integrate the editors successfully.

## Step 1. Download and Install Document Server

First, download the [**ONLYOFFICE Editors**](https://api.onlyoffice.com/editors/demopreview) (the ONLYOFFICE Document Server).

See the detailed guide to learn how to install Document Server [for Windows](https://helpcenter.onlyoffice.com/installation/docs-developer-install-windows.aspx?from=api_ruby_example), [for Linux](https://helpcenter.onlyoffice.com/installation/docs-developer-install-ubuntu.aspx?from=api_ruby_example), or [for Docker](https://helpcenter.onlyoffice.com/server/developer-edition/docker/docker-installation.aspx?from=api_ruby_example).

## Step 2. Install the prerequisites and run the web site with the editors

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

## Step 3. Checking accessibility

In case the example and Document Server are installed on different computers, make sure that your server with the example installed has access to the Document Server with the address which you specify instead of **documentserver** in the configuration files. And you must also make sure that the Document Server in its turn has access to the server with the example installed with the address which you specify instead of **example.com** in the configuration files.

If you integrated the editors successfully the result should look like the [demo preview](https://api.onlyoffice.com/editors/demopreview#DemoPreview) on our site.