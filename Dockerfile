FROM openjdk:8-jdk-slim-stretch

RUN apt-get update && apt-get install -y \
  curl \
  apt-transport-https \
  gnupg

RUN echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list
RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823
RUN apt-get update && apt-get install -y \
  sbt

RUN curl -sL https://deb.nodesource.com/setup_9.x | bash -
RUN apt-get update && apt-get install -y \
  nodejs

RUN mkdir -p /opt/newsleak
WORKDIR /opt/newsleak

ADD . /opt/newsleak

EXPOSE 9000

RUN sh init-repo.sh
RUN sbt compile
CMD ["sbt", "run"]
