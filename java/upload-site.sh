#!/bin/bash
WD=`pwd`
VERSION=`mvn -f $WD/java/pom.xml -q -Dexec.executable="echo" -Dexec.args='${project.version}' --non-recursive exec:exec`
aws s3 sync $WD/java/target/site/ s3://mtnfog-public/philter-sdk/java/$VERSION/
