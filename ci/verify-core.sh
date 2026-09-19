#!/usr/bin/env bash
set -euo pipefail

rm -rf build/self-test
mkdir -p build/self-test

javac --release 17 --add-modules jdk.httpserver -encoding UTF-8 -d build/self-test \
  download-core/src/main/java/com/kroxaboom/skazka/download/*.java \
  tests/DownloadCoreSelfTest.java \
  tests/HttpTransferSelfTest.java \
  tests/WorkerRegistrySelfTest.java \
  tests/HostThrottleSelfTest.java \
  tests/TransferRateMeterSelfTest.java

if grep -R -a -l -E 'java/lang/Record|java/lang/runtime/ObjectMethods' build/self-test/com/kroxaboom/skazka/download >/tmp/skazka-download-record-refs.txt; then
  cat /tmp/skazka-download-record-refs.txt >&2
  echo 'FAIL: Android 13-incompatible record bytecode in download core' >&2
  exit 1
fi
echo 'ANDROID_13_BYTECODE_COMPAT_OK'

java -cp build/self-test DownloadCoreSelfTest
java --add-modules jdk.httpserver -cp build/self-test HttpTransferSelfTest
java -cp build/self-test WorkerRegistrySelfTest
java -cp build/self-test HostThrottleSelfTest
java -cp build/self-test TransferRateMeterSelfTest
