#!/bin/bash
VERSION=`mvn -q -Dexec.executable="echo" -Dexec.args='${project.version}' --non-recursive exec:exec`
aws s3 sync ./target/site/ s3://mtnfog-public/philter-sdk/java/$VERSION/ --delete
