---
author: gwiedemann
date: 2019-04-17 11:14:44+00:00
draft: false
title: Installation
type: page
menu: main.documentation
weight: 24
meta: false
---

[![newsleak](http://newsleak.io/wp-content/uploads/2016/03/cropped-logo-draft.png)](https://uhh-lt.github.io/newsleak-frontend/install/)

## User Setup

To run newsleak, you need to install two dependent projects first.

### Docker

We distribute newsleak as a [Docker](https://www.docker.com) setup, orchestrated by `docker-compose`.

So please, install first `docker` and `docker-compose` on your system.

### Hoover

For easy data import, we rely on the Hoover toolset. To import data into newsleak, you must first have it imported into a local instance of Hoover.

To install Hoover and import a test collection, please follow the instructions on [this Github page](https://github.com/hoover/docker-setup).

### Newsleak

After a successfull installation and data import in Hoover, you can install and run newsleak.

For this, follow the instructions on [this Github page](https://github.com/uhh-lt/newsleak-docker).


## Development setup

[will be added soon...]

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