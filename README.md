# NEW/S/LEAK
[![Project status](https://img.shields.io/badge/status-active-brightgreen.svg)](#status)
[![Project Licence](https://img.shields.io/badge/licence-AGPL-blue.svg)](#license)

![newsleak](http://newsleak.io/wp-content/uploads/2016/03/cropped-logo-draft.png)

Science and Data-Driven Journalism: Data Extraction and Interactive Visualization of Unexplored Textual Datasets for Investigative Data-Driven Journalism (DIVID-DJ)



* [Project Description](https://www.inf.uni-hamburg.de/en/inst/ab/lt/research/divid-dj.html)
* [Blog](http://newsleak.io)
* [Documentation](https://tudarmstadt-lt.github.io/newsleak-frontend/scaladoc/index.html)

## Project Setup

#### Resolve external Javascript dependencies

The external Javascript dependencies of the project are resolved with [bower](https://bower.io/), whereas `bower` is fetched via [npm](https://www.npmjs.com/). To install the package manager `npm` for an Ubuntu based system execute the following commands. This will install the package manager using the local software repository.

```
 sudo apt-get update
 sudo apt-get install nodejs npm

```
Once `npm` is installed, execute the shell script `init-repo.sh`, which will download `bower` and resolve the Javascript dependencies to `app/assets/javascripts/libs/`. The list of Javascript dependencies is defined in `bower.json`.

#### Enter database and elasticsearch credentials

The application uses the file `conf/application.conf` for configuration. Fill in your database and elasticsearch credentials next to the section `Database configuration` obtained during the newsleak pipeline setup.

See the [Play database documentation](https://www.playframework.com/documentation/2.5.x/ScalaDatabase) for more information.

## Build Instructions

The application uses the build tool `sbt`, which is similar to Java's Maven or Ant and provides native support for compiling Scala code. Download the tool [here](http://www.scala-sbt.org/). Installation instructions for Linux are provided [here](http://www.scala-sbt.org/release/docs/Manual-Installation.html).

In order to compile the code run `sbt compile` in the root directory of the application. This will fetch all external Scala dependencies and further resolves the Scala compiler interface. To run the application in development mode execute `sbt run` and open the application in the browser via `localhost:9000`. In this mode, Play will check your your project and recompile required sources for each request. **The development mode is not intended for a productive environment!**

## Deployment

The following explains how to deploy the application in productive mode using the sbt `dist` task. This dist task creates a binary version of the application, which can be deployed to a server. To run the binary on a server a [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html) installation is required.

In order to create the binary, run `sbt dist` in the root directory of the application. This will package all required files to a zip archive, which will be stored in `target/universal`. For more information see the [Play documentation](https://www.playframework.com/documentation/2.5.x/Production).

To deploy the binary on the server, unpack the packaged zip archive (`target/universal/`) to a directory of your choice e.g. `unzip new-s-leak-1.0.2.zip -d /path/` unpacks the archive to `/path/`. Next modify the `conf/application.conf` file to provide the database and elasticsearch credentials.

Finally, execute `bin/new-s-leak -Dconfig.file=conf/application.conf` from the root folder of your application. The application is then reachable via port `9000`. Also make sure this port is reachable from the outside.

## The new/s/leak Pipeline
We have compiled everything required to start the new/s/leak application. You can download the required resources from [here](). Unzip the zip file and we call this **NEWSLEAKHOME**

### System requirements and Configurations
1. Java 8 and above
2. PostgresSQL 9.1 and above. Make sure you have a user with roles such as createdb and createrole. If you do not have one already, you can create as follows
```
CREATE ROLE newsreader LOGIN ENCRYPTED PASSWORD 'md520828c83196619e0230b4f434489680b' SUPERUSER INHERIT CREATEDB CREATEROLE ;
CREATE DATABASE newsreader WITH OWNER = postgres ENCODING = 'UTF8' TABLESPACE = pg_default LC_COLLATE = 'en_US.UTF-8' LC_CTYPE = 'en_US.UTF-8' CONNECTION LIMIT = -1;
```

Before starting the application complete the followings:

 * Prepare your document as it is seen in the sample `document.csv` file. It should be well-formatted CSV file as `"DocID"`,`Content"`,`"CreationDate"`.  `"DocID"` should be a number, and `"CreationDate"` is a proper date such as `2010-01-01`, `2010-10`, `2010`
 * Prepare the metadata file as it is seen in the sample `metadata.csv` file. It should be well-formatted CSV file as `"DocID"`,`"MetadataKey"`,`"MetadataValue"`, `"MetadataType"`. `MetadataKey` is the name of the metadata and `MetadataValue` is the value of the metadata while `MetadataType` is the data type of the meta data such as Text, Numeric or Date.
 * Edit the `newsleak.properties` file.
 Set the different configurations accordingly.

###### NEWSLEAK CONFIG SECTIONS

 Set the `dbname` to a new database name for new/s/leak. Example
 ```
 dbname = newsleak
 ```
 Set the `dbuser` , `dbpass` and the `dbaddress` accordingly. This user should be superuser created as above in the system requirements section. Example
 ```
 dbuser = newsreader
 dbpass = newsreader
 dbaddress = localhost:5432
 ```
Set the name of elasticsearch index you plan to use. See below about Elasticsearch configurations
```
indexname = newsleak
```
Set `lang` either to `en` for English documents or `de` for German documents.
```
lang = en
```
Set `documentname` to the name of the document as it is specified in (1.) above.
```
documentname = document.csv
```
Finally set `threads` to a number of threads you want to use to have parallel event time extraction. 10 might be enough for a single CPU with 4 cores. See details below to experiment with different threads for time expression
###### HEIDLTIME CONFIG SECTIONS
The only configuration you have to change in this section is the `treeTaggerHome`. Provide an absolute path to the `TreeTager` folder which is found under `NEWSLEAKHOME`.

* Open the `application.conf` file and make changes to the `Database configuration` section Example
  ```
  db.newsleak.driver=org.postgresql.Driver
  db.newsleak.url="jdbc:postgresql://localhost:5432/newsleak"
  db.newsleak.username="newsreader"
  db.newsleak.password="newsreader"
  ```
  Also, change to the elastic search related configurations. The appropriate elasticsearch version supported is available under `NEWSLEAKHOME`. If you plan your existing elasticsearch installation, make sure they have the same versions (`2.2.0`)
Make sure also you have the same `clustername` in the `elasticsearch.yml` file as shown below.
````
es.clustername = "NewsLeaksCluster"
es.address = "localhost"
es.port = 9501
es.indices =  [newsleak]
es.index.default = "newsleak"

````

* Edit the `elasticsearch.yml` so that the name of the cluster matches the name given in the `application.conf` file.
```
cluster.name: NewsLeaksCluster
```
Also give an available ports and bind address for Elasticsearch.
```
network.host: 0.0.0.0
transport.tcp.port: 9501
http.port: 9500
```
Finally, provide the absolute path of Elasticsearch for `path.data` and `path.home`
```
 path.data: /ABSOLUTE/PATH/TO/elasticsearch-2.2.0
 path.home: /ABSOLUTE/PATH/TO/elasticsearch-2.2.0
 ```

### Start the preprocessing components
Once the above configurations are in place, start the pre-processing component as follows

* To run all preprocessing components (entity extraction, import to database, build elasticsearch index...) run the following

```
java -jar newsleakprocessor.jar -a
```
* Otherwise, you can run the components separately, but in the order provided as floows
```
1. java -jar newsleakprocessor.jar -h  //// Extract event time expressions
2. java -jar newsleakprocessor.jar -g //// Extract named entities
3. java -jar newsleakprocessor.jar -d //// Import data to database
4. java -jar newsleakprocessor.jar -e //// Build elastic search index
```

### Start new/s/leak application
Once the pre=process components are completed, you can start new/s/leak application as follows
1. First, copy the modified `elasticsearch.yml` file into `NEWSLEAKHOME/elasticsearch-2.2.0/config/` and start elastic search as follows
```
./elasticsearch-2.2.0/bin/elasticsearch
```
2. start new/s/leak as follows
```
bin/new-s-leak -Dconfig.file=application.conf
```
## Want to help?

Want to find a bug, contribute some code, or improve documentation? Read up on our guidelines for [contributing](https://github.com/tudarmstadt-lt/newsleak/blob/master/CONTRIBUTING.md) and then check out one of our issues.

## License

```
Copyright (C) 2016 Language Technology Group and Interactive Graphics Systems Group, Technische Universität Darmstadt, Germany

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
```
