FROM openjdk:11.0.2-jdk-slim-stretch

RUN apt-get update && apt-get install -y \
  curl \
  apt-transport-https \
  gnupg

RUN groupadd -g 999 newsleak && useradd -r -u 999 -g newsleak newsleak

RUN mkdir -p /opt/newsleak
RUN chown newsleak:newsleak /opt/newsleak

WORKDIR /opt/newsleak

ADD target/universal/newsleak-ui .
RUN rm conf/*.conf
ADD conf/application.production.conf conf/

ADD preprocessing/target/preprocessing-jar-with-dependencies.jar preprocessing.jar
ADD preprocessing/conf/dictionaries conf/dictionaries/
ADD preprocessing/conf/newsleak.properties conf/
ADD preprocessing/data/document_example.csv data/document_example.csv
ADD preprocessing/data/metadata_example.csv data/metadata_example.csv
ADD preprocessing/resources resources/
ADD preprocessing/desc desc/

RUN chown newsleak:newsleak /opt/newsleak/data

ADD newsleak-start.sh .

USER newsleak

EXPOSE 9000

CMD ./newsleak-start.sh
