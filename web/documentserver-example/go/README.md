## Overview

This example will help you integrate ONLYOFFICE Docs into your web application written in Go.

It is aimed at testing the editors. Please, do not use it for production without proper modifications.
## Installation

### Step 1. Install ONLYOFFICE Docs

Download and install ONLYOFFICE Docs (packaged as Document Server).

See the detailed guide to learn how to install Document Server [for Windows](https://helpcenter.onlyoffice.com/installation/docs-developer-install-windows.aspx).

### Step 2. Download the Go code for the editors integration

Download the [Go example](https://api.onlyoffice.com/editors/demopreview) from our site.

To connect the editors to your website, specify the path to the editors installation, server protocol, address and port  in the *configuration.env* file:

```
SERVER_ADDRESS=http(s)://address (optional)
SERVER_PORT=port

DOC_SERVER_HOST=http://documentserver/

JWT_IS_ENABLED=flag
JWT_SECRET=secret
JWT_HEADER=Authorization
```

where the **documentserver** is the name of the server with the ONLYOFFICE Document Server installed.
**address** is the address of the server, **port** is the server port.

If you want to experiment with the editor configuration, modify the [parameters](https://api.onlyoffice.com/editors/advanced) it the *templates/editor.html* file.

### Step 3. Install the prerequisites
To run the Go example code, install the Go compiler:

* Go (download from [The Go lang website](https://golang.org/);

### Step 4. Set environment variables

Having installed the compiler, update golang env variables:

```
export GOPATH=$HOME/go
export PATH=$PATH:$GOPATH/bin
export PATH=$PATH:/usr/local/go/bin
```

### Step 5. Configure JWT 

Open the *config/configuration.json* file and enable JWT:

```
{
    "JWT_IS_ENABLED" : true,
}
```

Also, [specify the same secret key](https://helpcenter.onlyoffice.com/installation/docs-configure-jwt.aspx) as used in your Document Server: 

```
{
    "JWT_SECRET" : "secret",
}
```


### Step 6. Start the application

1. Go to the project root.
2. Run:
    ```
    go run main.go
    ```
3. In your browser go to **server.address** and **server.port**:

### Step 7. Check accessibility

 In case the example and Document Server are installed on different computers, make sure that your server with the example installed has access to the Document Server with the address which you specify instead of **documentserver** in the configuration files.
