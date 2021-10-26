# GitHub Activity Application

This project aims at centralizing all the information about the status of QuarkusIO activities in upstream.

## Setup
In your application directory, create a .env file containing your OAuth token:

```
ACTIVITY_TOKEN=<TOKEN>
```

The token only needs read access to the repository.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
mvn compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
mvn package
```
The application is now runnable using `ACTIVITY_TOKEN=your_token java -jar target/quarkus-app/quarkus-run.jar`.

## Creating a native executable
You can create a native executable using: 
```shell script
mvn package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
mvn package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `ACTIVITY_TOKEN=your_token ./target/gh-activity-1.0.0-SNAPSHOT-runner`

## Deploying to OpenShift
JVM build:
```shell
mvn clean package -Dquarkus.kubernetes.deploy=true
```
Native build:
```shell
mvn clean package -Dquarkus.kubernetes.deploy=true -Dquarkus.native.container-build=true -Dnative
```

OpenShift secret for GitHub API needs to be created beforehand.
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: gh-activity-token
stringData:
  ACTIVITY_TOKEN: your_token
```

Deploy the secret using `oc create -f secret.yaml` command, update it using `oc replace -f secret.yaml` command. 

# Testing

Github Graphql Site to test your queries: `https://docs.github.com/en/graphql/overview/explorer`
Public Graphql Schema: https://docs.github.com/en/graphql/overview/public-schema