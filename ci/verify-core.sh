#!/usr/bin/env bash
set -euo pipefail

rm -rf build/self-test
mkdir -p build/self-test

javac -encoding UTF-8 -d build/self-test \
  download-core/src/main/java/com/kroxaboom/skazka/download/*.java \
  tests/DownloadCoreSelfTest.java

java -cp build/self-test DownloadCoreSelfTest
