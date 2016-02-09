#!/bin/bash -ex
# Downloads the latest Intellij IDEA Community jars required for script builds

JSON=`wget -qO - 'https://data.services.jetbrains.com/products/releases?code=IIC&latest=true&type=release'`
URL=`node -p -e "($JSON).IIC[0].downloads.linux.link"`
wget -O idea.tar.gz $URL
tar zxvf idea.tar.gz --wildcards --no-wildcards-match-slash 'idea-IC-*/lib/*.jar'
mv idea-IC-* idea
