#!/bin/sh

XMAGE_CONFIG=/xmage/config/config.xml

sed -i -e "s#\(serverAddress=\)[\"].*[\"]#\1\"$XMAGE_DOCKER_SERVER_ADDRESS\"#g" ${XMAGE_CONFIG}
sed -i -e "s#\(serverName=\)[\"].*[\"]#\1\"$XMAGE_DOCKER_SERVER_NAME\"#g" ${XMAGE_CONFIG}
sed -i -e "s#\(port=\)[\"].*[\"]#\1\"$XMAGE_DOCKER_PORT\"#g" ${XMAGE_CONFIG}
sed -i -e "s#\(secondaryBindPort=\)[\"].*[\"]#\1\"$XMAGE_DOCKER_SECONDARY_BIND_PORT\"#g" ${XMAGE_CONFIG}
sed -i -e "s#\(maxSecondsIdle=\)[\"].*[\"]#\1\"$XMAGE_DOCKER_MAX_SECONDS_IDLE\"#g" ${XMAGE_CONFIG}
sed -i -e "s#\(authenticationActivated=\)[\"].*[\"]#\1\"$XMAGE_DOCKER_AUTHENTICATION_ACTIVATED\"#g" ${XMAGE_CONFIG}

# Resolve the server JAR path (glob won't expand reliably inside exec)
MAGE_SERVER_JAR=$(ls ./lib/mage-server*.jar 2>/dev/null | head -1)
if [ -z "$MAGE_SERVER_JAR" ]; then
    echo "ERROR: mage-server JAR not found in ./lib/"
    ls -la ./lib/
    exit 1
fi

exec java \
    -Xms${JAVA_MIN_MEMORY} \
    -Xmx${JAVA_MAX_MEMORY} \
    -Djava.security.policy=./config/security.policy \
    -Dlog4j.configuration=file:./config/log4j.properties \
    -jar "$MAGE_SERVER_JAR"
