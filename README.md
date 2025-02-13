# CI-Server Group 2
This project is built from https://github.com/KTH-DD2480/smallest-java-ci
It is a CI-server that is meant to be called as a webhook by Github that can be compiled and run using Maven.



## Install Dependencies and Run CI-Server:


First is to check out the repository :
```
git clone https://github.com/jolinefrisk/ci-server
cd ci-server
```


to implement the server  is to make sure that all dependencies are installed

Apache Marven 3.9.9
Installation [guide for Maven](https://maven.apache.org/install.html)

ngok
Installation [guide for ngok](https://ngrok.com/docs/guides/device-gateway/linux/)

#### 1. Compile the project:

```
mvn clean package
```

#### 2. Start the server:

```
java -jar target/ci-server-1.0-SNAPSHOT.jar
```

#### 3. Expose the server via Ngrok:

Open a new terminal window and run:

```
grok http 8080
```

#### 4. Connect CI-server to the Repository:

Ngrok will generate a URL that can be used to connect the server to a Webhook.

To configure the Github Repository:
go to Settings >> Webhooks, click on Add webhook.
Paste the forwardinf URL you got from the termenal (exaple http://8929b010.ngrok.io) in field Payload URL) click on Add webbhock. 
To thest that evrything wokrs go to http://localhost:8080 tp check that the CI server is running locally

## Code logic

## Essence standard evaluation

## Statement of contributions
