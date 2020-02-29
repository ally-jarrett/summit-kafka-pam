#!/usr/bin/env bash

echo "Downloading GraalVM"
wget -q https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-19.3.1/graalvm-ce-java8-linux-amd64-19.3.1.tar.gz
tar zxf graalvm-ce-java8-linux-amd64-19.3.1.tar.gz

echo "Installing GraalVM via gu"

${CI_PROJECT_DIR}/graalvm-ce-java8-19.3.1/bin/gu install native-image