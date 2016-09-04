new/s/leak
==========

**new/s/leak** represents the front end layer application of the distributed software package newsleak ("Network of searchable Leaks").

* Web site: [http://newsleak.io](http://newsleak.io)


## 1. Build Instructions


Make sure you have the build tool `sbt` installed. It can be downloaded [here](http://www.scala-sbt.org/). As minimum sbt 0.13.11 and scala 2.11 are required.

The Application is running on Play Framework 2.5

Also [nodeJS](https://nodejs.org/) and npm (comes with nodejs or linux package manager) needs to be installed.

* Execute the script `init-repo.sh`. This will install the package manager `bower` using `npm`. That will further install Javascript dependencies. Both are dependency management tools, whereas `bower` can be used to manage front end components like html, css, js etc and `npm` is used for installing Node js modules.

  **Note**: During the installation process you will be asked to resolve various version problems.
  
* Add newsleak [backend API](https://github.com/tudarmstadt-lt/newsleak) to your local ivy repository using `sbt publishLocal` in newsleak backend API folder

  **Note**: There is an dependency conflict between `commons-codec 1.10` and `commons-codec 1.5`. The backend API will be forced to use version `1.10`.

* Run `sbt compile run` and open the application in the browser: `localhost:9000`.

* Run `sbt dist` to package all needed Files into one zip file located in `target/universal/`


## 2. Deployment

Before you install the Application, make sure, you have a postgresql server with a valid database running.

To install the application you have to unpack the previously with `sbt dist` packaged file into a directory of your choice. This can be done with the `unzip new-s-leak-1.0.2.zip` program. Next you can modify the `conf/application.conf` file to provide the database settings to the application.

* The option db.default.url modifies the current URL to connect to the database

* The options db.default.username and db.default.password are modifying the login data to the database

* The database needs to be a postgre database

After this is done, move to the root folder of the application and run `bin/new-s-leak -Dconfig.file=conf/application.conf`. After this the Application should be reachable over Port 9000.

Alternatively you can use the provided upstart script on the nwdt server. It can be found in the `bin` folder in the homedirectory on the nwdt server. you can use it as followed

* The execution of `upstart-nwdt.sh start` starts all services needed to run newsleak and newsleak

* The execution of `upstart-nwdt.sh restart` restarts all services and newsleak

* The execution of `upstart-nwdt.sh stop` stops all services and newsleak


## Want to help?

Want to find a bug, contribute some code, or improve documentation? Read up on our guidelines for [contributing](TODO) and then check out one of our issues.

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
[NoDCore Component](https://github.com/Tooa/NoDCore)
