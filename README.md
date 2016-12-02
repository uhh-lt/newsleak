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
