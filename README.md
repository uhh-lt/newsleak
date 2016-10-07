new/s/leak
==========

**new/s/leak** represents the front-end layer application of the distributed software package newsleak ("Network of searchable Leaks").

* Website: [http://newsleak.io](http://newsleak.io)


## 1. Build Instructions


Make sure you have the build tool `sbt` installed. It can be downloaded [here](http://www.scala-sbt.org/). As a minimum requirement sbt 0.13.11 and scala 2.11 are required.

The Application uses the Play Framework 2.5.

Furthermore, [nodeJS](https://nodejs.org/) and npm (served with nodejs or the linux package manager) needs to be installed.

* Execute the script `init-repo.sh`. This will install the package manager `bower` using `npm` and further resolves all Javascript dependencies. Both applications are dependency management tools, whereas `bower` can be used managing front-end components like html, css, js, etc.  and `npm` is used for installing Node.JS modules.

  **Note**: During the installation process you will be asked to resolve various version problems.
  
* Add newsleak [backend API](https://github.com/tudarmstadt-lt/newsleak) to your local ivy repository by executing `sbt publishLocal` in the newsleak backend API folder.

* Run `sbt compile run` and open the application in the browser: `localhost:9000`.

* Run `sbt dist` to package all required files to a zip file, which will be stored in `target/universal/`. See the [Play documentation](https://www.playframework.com/documentation/2.5.x/Production) for more information.


## 2. Deployment

Before deploying the application, make sure, you have a postgresql server with a valid database schema running.

To install the application, unpack the packaged zip file (`target/universal/`) to a directory of your choice. For example, `unzip new-s-leak-1.0.2.zip -d /path/` unpacks the zip file to `/path/`. Next you need to modify the `conf/application.conf` file to provide the database settings to the application. See the [Play database documentation](https://www.playframework.com/documentation/2.5.x/ScalaDatabase) for more information.

* The option `db.default.url` modifies the current URL to connect to the database.

* The options `db.default.username` and `db.default.password` modify the login data for the database.

* The backend API utilizes a postgre database. Other databases can be used by implementing the backend API interfaces for other database technologies. 

Finally, run `bin/new-s-leak -Dconfig.file=conf/application.conf` from the root folder of your application. Subsequently, the application is reachable via port `9000`.

Alternatively, use the provided upstart script on the nwdt server. The script is stored in the `bin` folder in the home directory of the nwdt server. The script works as following:

* The execution of `upstart-nwdt.sh start` starts all services required to run newsleak and also starts the application.

* The execution of `upstart-nwdt.sh restart` restarts all services and the application itself.

* The execution of `upstart-nwdt.sh stop` stops all services and newsleak as well.


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

See also
--------
[Newsleak Backend API](https://github.com/tudarmstadt-lt/newsleak)
