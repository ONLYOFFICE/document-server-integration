## Overview

This example will help you integrate ONLYOFFICE Docs into your web application written in Java 
with Spring Boot.

Spring Boot has a lot of functionality, but its most significant features are: dependency management, 
auto-configuration, and built-in servlet containers.

**Please note**: It is intended for testing purposes and demonstrating functionality of the editors. Do NOT use this integration example on your own server without proper code modifications! In case you enabled the test example, disable it before going for production.

## For Windows

### Step 1. Install ONLYOFFICE Docs

Download and install ONLYOFFICE Docs (packaged as Document Server).

See the detailed guide to learn how to install Document Server [for Windows](https://helpcenter.onlyoffice.com/installation/docs-developer-install-windows.aspx). 

### Step 2. Download the Java code for the editors integration

Download the [Java-Spring example](https://api.onlyoffice.com/editors/demopreview) from our site.

To connect the editors to your website, specify the path to the editors installation, server port and the path to the storage folder in the *src/main/resources/application.properties* file:

```
 files.storage=
 server.port=port
 files.docservice.url.site=https://documentserver/
```

where the **documentserver** is the name of the server with the ONLYOFFICE Docs installed, **port** is any available port and **files.storage** is the path where files will be created and stored (in the project folder by default). You can set an absolute path. For example, *D:\\\\folder*. Please note that on Windows OS the double backslash must be used as a separator.

If you want to experiment with the editor configuration, modify the [parameters](https://api.onlyoffice.com/editors/advanced) it the *\src\main\resources\editor.html* file.

### Step 3. Install the prerequisites
To run the Java example code, install the Java version 11 appropriate for your OS and framework  **Apache Maven**:

* Java (download from [the Oracle official website](https://www.oracle.com/ru/java/technologies/javase-jdk11-downloads.html));
* Apache Maven (download from [the official website](https://maven.apache.org/download.cgi)).

### Step 4. Set environment variables

1. After you have installed Java on Windows, set the **JAVA_HOME** environment variable to point to the Java installation directory.

	Find out where Java is installed. If you didn't change the path during installation, it will be something like this:

	```
	C:\Program Files\Java\jdk11
	```

	In **Windows 7** right click **My Computer** and select **Properties**, then click **Advanced**.

	In **Windows 8** go to **Control Panel** -> **System** -> **Advanced System Settings**.

	Click the **Environment Variables** button.

	Under **System Variables**, click **New**.

	In the **Variable Name** field, enter **JAVA_HOME** if you installed the **JDK** (Java Development Kit) or **JRE_HOME** if you installed the **JRE** (Java Runtime Environment).

	In the **Variable Value** field, enter your **JDK** or **JRE** installation path, for example C:\Program Files\Java\jdk11.

	Check if the variable created successfully by **echo** command in the **Command Prompt**:

	```
	echo %JAVA_HOME%
	```

2. Set the **MAVEN_HOME** environment variable:

    Unzip the downloaded archive with the maven to any directory, it will be something like this:

    ```
    C:\apache-maven-3.8.1
   ```
    In **Windows 7** right click **My Computer** and select **Properties**, then click **Advanced**.

	In **Windows 8** go to **Control Panel** -> **System** -> **Advanced System Settings**.

	Click the **Environment Variables** button.

	Under **System Variables**, click **New**.

	In the **Variable Name** field, enter **MAVEN_HOME**.

	In the **Variable Value** field, enter your **JDK** or **JRE** installation path, for example C:\apache-maven-3.8.1.

	Add C:\apache-maven-3.8.1\bin to the PATH system variable:
	In system variables, find PATH, clicks on the Edit... button. In “Edit environment variable” dialog, clicks on the New button and add this C:\apache-maven-3.8.1\bin

	Check if the variable created successfully by **echo** command in the **Command Prompt**:

	```
	echo %MAVEN_HOME%
	```


### Step 5. Start application with Maven

1. Open the console and go the java-spring folder using the **cd** command, for example:
	```
	cd C:\Program Files\document-server-integration\web\documentserver-example\java-spring
	```
2. In the open console enter the following commands:
	```
	mvn clean
	mvn package
	mvn spring-boot:run
	```
3. Open your browser using **server.address** and **server.port**:
 
     ```
     http://server.address:server.port/
     ```

### Step 6. Check accessibility

 In case the example and Document Server are installed on different computers, make sure that your server with the example installed has access to the Document Server with the address which you specify instead of **documentserver** in the configuration files. 

Make sure that the Document Server has access to the server with the example installed with the address which you specify instead of **example.com** in the configuration files.

## For Linux
### Step 1. Install ONLYOFFICE Docs

Download and install ONLYOFFICE Docs (packaged as Document Server). 

See the detailed guide to learn how to install Document Server [for Linux](https://helpcenter.onlyoffice.com/installation/docs-developer-install-ubuntu.aspx). 

### Step 2. Install the prerequisites and run the website with the editors

1. Install **Java** following the instructions [here](https://docs.oracle.com/en/java/javase/20/install/installation-jdk-linux-platforms.html#GUID-737A84E4-2EFF-4D38-8E60-3E29D1B884B8).

2. Download the archive with the Java-Spring example and unpack the archive or clone git repository:

    a) archive with Java-Spring:

    ```
    wget https://api.onlyoffice.com/app_data/editor/Java.Spring.Example.zip
    ```
    
    ```
    unzip Java.Spring.Example.zip
    ```
    b) git repository:
    ```
    git clone https://github.com/ONLYOFFICE/document-server-integration.git
    ```


3. Change the current directory for the project directory:

    a) from archive

    ```
   cd Java\ Spring\ Example/
   ```
   b) from git repository 
    ```
    cd document-server-integration/web/documentserver-example/java-spring
    ```
4. Edit the *src/main/resources/application.properties* configuration file. Specify the name of your local server with the ONLYOFFICE Document Server installed.

    ```
    nano src/main/resources/application.properties
    ```

	Edit the following lines:

    ```
    files.storage=
    server.port=port
    files.docservice.url.site=https://documentserver/
    ```

	where the **documentserver** is the name of the server with the ONLYOFFICE Docs installed, **port** is any available port and **files.storage** is the path where files will be created and stored (in the project folder by default). You can set an absolute path.
   

5. Install **Maven**:

    ```
    sudo apt-get install maven
    ```
    
6. Build:

    ```
    mvn package
    ```

 7. Start Java-Spring example:
     ```
     ./mvnw spring-boot:run
     ```
 8. Open your browser using **server.address** and **server.port**:

     ```
     http://server.address:server.port/
     ```
  

### Step 3. Check accessibility

In case the example and Document Server are installed on different computers, make sure that your server with the example installed has access to the Document Server with the address which you specify instead of **documentserver** in the configuration files. 

Make sure that the Document Server has access to the server with the example installed with the address which you specify instead of **example.com** in the configuration files.

##  For Docker

### Step 1. Install ONLYOFFICE Docs

Download and install ONLYOFFICE Docs (packaged as Document Server). 

See the detailed guide to learn how to install Document Server [for Docker](https://helpcenter.onlyoffice.com/installation/docs-developer-install-docker.aspx). 

### Step 2. Install the prerequisites and run the website with the editors

1. Install **Java** following the instructions [here](https://docs.oracle.com/en/java/javase/20/install/installation-jdk-linux-platforms.html#GUID-737A84E4-2EFF-4D38-8E60-3E29D1B884B8).

2. Download the archive with the Java-Spring example and unpack the archive or clone git repository:

    a) archive with Java-Spring:

    ```
    wget https://api.onlyoffice.com/app_data/editor/Java.Spring.Example.zip
    ```
    
    ```
    unzip Java.Spring.Example.zip
    ```
    b) git repository:
    ```
    git clone https://github.com/ONLYOFFICE/document-server-integration.git
    ```


3. Change the current directory for the project directory:

    a) from archive

    ```
   cd Java\ Spring\ Example/
   ```
   b) from git repository 
    ```
    cd document-server-integration/web/documentserver-example/java-spring
    ```
4. Edit the *src/main/resources/application.properties* configuration file. Specify the name of your local server with the ONLYOFFICE Document Server installed:

	```
	nano src/main/resources/application.properties
	```
	
5. Edit the following lines:

   ```
   files.storage=
   server.port=port
   files.docservice.url.site=https://documentserver/
   ```

    where the **documentserver** is the name of the server with the ONLYOFFICE Docs installed, **port** is any available port and **files.storage** is the path where files will be created and stored (in the project folder by default). You can set an absolute path.

6. Run the next command in the java example directory:

	```
	docker-compose up
	```
7. Open your browser using **server.address** and **server.port**:

      ```
      http://server.address:server.port/
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