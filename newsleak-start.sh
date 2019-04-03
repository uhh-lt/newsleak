#!/bin/bash
cd /opt/newsleak
rm -f RUNNING_PID
cp -n -r conf /etc/settings/
chmod -R 777 /etc/settings/conf
export NEWSLEAK_CONFIG=/etc/settings/conf/application.production.conf
bin/newsleak -Dconfig.file=/etc/settings/conf/application.production.conf
