#!/bin/bash -x
# Downloads the latest Intellij IDEA Community jars required for script builds

JSON=`wget -qO - 'https://data.services.jetbrains.com/products/releases?code=IIC&latest=true&type=release'`
URL=`node -p -e "($JSON).IIC[0].downloads.linux.link"`
wget -qO - $URL | tar zxv --wildcards --no-wildcards-match-slash 'idea-IC-*/lib/*.jar'
mv idea-IC-* idea
