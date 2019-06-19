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

### Start dev service containers

Stop the newsleak containers from the docker user setup (step 1 above) and restart them with the dev container orchestration:

```
cd newsleak-docker
docker-compose down
docker-compose -f docker-compose.dev.yml up -d
```

This omits starting the `newsleak-ui` container and exposes ports of the remaining three containers: `newsleak-postgres`, `newsleak-elasticsearch`, and `newsleak-ner`.

### Setup UI application

The user interface is a front-end/back-end web application based on the Scala [Play framework](https://www.playframework.com/).

Make sure you have a setup with preprocessed data (e.g. the test collection from the [user installation](/install)).

To configure the scala application, change the settings in `conf/application.conf` to match the url and exposed ports of your newsleak containers. 

```
nano conf/application.conf
```

... among others, it look like this (important paramerters are `es.indices`, and `es.index.default` pointing to the `newsleakdev` db configuration below; also take care of opened ports from the newsleak-docker dev service containers `es.address=localhost` and `es.port=19300`, and `db.newsleakdev.url` accordingly):

```
# Available ES collections (provide db.* configuration for each collection below)
es.indices =  [newsleakdev,newsleak2,newsleak3,newsleak4,newsleak5]
# ES connection
es.clustername = "elasticsearch"
es.address = "localhost"
es.port = 19300

# Determine the default dataset for the application
es.index.default = "newsleakdev"

# collection 1
db.newsleakdev.driver=org.postgresql.Driver
db.newsleakdev.url="jdbc:postgresql://localhost:15432/newsleak"
db.newsleakdev.username="newsreader"
db.newsleakdev.password="newsreader"
es.newsleakdev.excludeTypes = [Link,Filename,Path,Content-type,SUBJECT,HEADER,Subject,Timezone,sender.id,Recipients.id,Recipients.order]
```

Then, compile the assets for the UI application (requires `nodejs` and `npm` installed on your system):

```
npm install
```

Alternatively to the command above, you can run the script file `./init-repo.sh`.

Finally, start the front-end application within the main `newsleak` project directory.

```
sbt run
```

And open `http://localhost:9000` in your browser.

### Debug frontend application

Run the UI application with enabled debugging server.

```
sbt -jvm-debug 9999 run
```

Finally, point your IDE in the debug mode to listen to the debug server.

### Setup preprocessing development

Open the `preprocessing` project (e.g. via "Import Maven project") of your cloned `uhh-lt/newsleak` project in your favourite development environment such as Eclipse or IntelliJ.

Make a copy of `preprocessing/conf/newsleak.properties`, e.g. `preprocessing/conf/newsleak_dev.properties`, and edit it to make sure you point to the right ports on `localhost` to reach the exposed docker container ports of the postgres db, the elasticsearch index, and the ner microservice.

In your IDE, run `CreateCollection` with the new dev configuration file in the `preprocessing` directory.

```
uhh_lt.newsleak.preprocessing.CreateCollection -c conf/newsleak_dev.properties
```

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


