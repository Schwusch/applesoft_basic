#!/usr/bin/env bash
set -ex

mvn clean install

native-image -cp ./target/kotlin-applesoft-basic-1.0-SNAPSHOT-jar-with-dependencies.jar -H:Name=AppleSoft -H:Class=main.MainKt -H:+ReportUnsupportedElementsAtRuntime
