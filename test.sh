#!/bin/bash
set -e

# Run Java integration tests
cd src
mvn install --quiet -Dmaven.test.skip=true -DskipTests=true
jar=$(ls -1 metabase-report-test/target//metabase-report-test-*.jar)
java -jar $jar $1
