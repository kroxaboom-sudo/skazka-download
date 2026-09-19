#!/usr/bin/env bash
set -euo pipefail

rm -rf build/self-test
mkdir -p build/self-test

javac --release 17 --add-modules jdk.httpserver -encoding UTF-8 -d build/self-test \
  download-core/src/main/java/com/kroxaboom/skazka/download/*.java \
  tests/DownloadCoreSelfTest.java \
  tests/HttpTransferSelfTest.java \
  tests/WorkerRegistrySelfTest.java

java -cp build/self-test DownloadCoreSelfTest
java --add-modules jdk.httpserver -cp build/self-test HttpTransferSelfTest
java -cp build/self-test WorkerRegistrySelfTest
