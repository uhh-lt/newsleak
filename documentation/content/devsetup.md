---
type: "page"
draft: false
author: "gwiedemann"
description: "development setup of newsleak"
keywords: ["installation", "development"]
tags: ["installation", "development"]
---

# Development setup

1. Follow the instructions for the [user installation](/install) to get a working hoover and newsleak installation.

2. Clone the newsleak Github repository

```
git clone https://github.com/uhh-lt/newsleak.git
```


### Preprocessing pipeline

The `preprocessing` pipeline is a Java-based Maven project using [Apache UIMA](https://uima.apache.org/) a base technology for information extraction.

Stop the newsleak containers and restart them with the dev container orchestration:

```
cd newsleak-docker
docker-compose down
docker-compose -f docker-compose.dev.yml up -d
```

This omits starting the `newsleak-ui` container and exposes ports of the remaining three containers: `newsleak-postgres`, `newsleak-elasticsearch`, and `newsleak-ner`.

Now you can open the `preprocessing` project (e.g. via "Import Maven project") of your cloned `uhh-lt/newsleak` project in your favourite development environment such as Eclipse or IntelliJ.

Make a copy of `preprocessing/conf/newsleak.properties`, e.g. `preprocessing/conf/newsleak_dev.properties`, and edit it to make sure you point to the right ports on `localhost` to reach the exposed docker container ports of the postgres db, the elasticsearch index, and the ner microservice.

In your IDE, run `CreateCollection` with the new dev configuration file in the `preprocessing` directory.

```
uhh_lt.newsleak.preprocessing.CreateCollection -c conf/newsleak_dev.properties
```

Then, change the settings for the UI application in `conf/application.conf` to match the url and exposed ports of your newsleak containers (e.g. `es.address=localhost` and `es.port=19300`, and `db.*` accordingly). 

Start the front-end application in the main `newsleak` project directory.

```
sbt run
```


### Frontend application

The user interface is a front-end/back-end web application based on the Scala [Play framework](https://www.playframework.com/).

Make sure you have a setup with preprocessed data (e.g. the test collection from the [user installation](/install)).

Stop the newsleak containers and restart them with the dev container orchestration:

```
cd newsleak-docker
docker-compose down
docker-compose -f docker-compose.dev.yml up -d
```

Then, change the settings for the UI application in `conf/application.conf` to match the url and exposed ports of your newsleak containers (e.g. `es.address=localhost` and `es.port=19300`, and `db.*` accordingly). 

Run the UI application with enabled debugging server.

```
sbt -jvm-debug 9999 run
```

Finally, point your IDE in the debug mode to listen to the debug server.

### Publish a docker image

A new version of of newsleak can be published with a version number on [docker hub](https://hub.docker.com/). Then the [newsleak-docker](https://github.com/uhh-lt/newsleak-docker) setup can automatically download this new version and make available for the user installation.

Compile preprocessing.jar

```
cd preprocessing
mvn clean package assembly:single
```


Compile dist version of newsleak UI:

```
cd ..
sbt dist
```

Build the docker image. Set the version name accordingly!

```
unzip target/universal/newsleak-ui.zip -d target/universal/
docker build -t uhhlt/newsleak:v1.0 .
```

Test the new image: in `newsleak-docker` run:

```
docker-compose up -d
docker exec -it newsleak sh -c "cd /opt/newsleak && java -Xmx10g -jar preprocessing.jar -c /etc/settings/conf/newsleak.properties"
```

If the container works as expected, push it to docker hub (again, set the correct version number!)

```
docker login
docker push uhhlt/newsleak:v1.0
```

And change the container version in the files `docker-compose.yml`, and `docker-compose.dev.yml` of `newsleak-docker` to match the new version:

```
  newsleak-ui:
    image: "uhhlt/newsleak:v1.0"
```


