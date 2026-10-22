#!/bin/sh
set -eu

if [ -n "${NOTIFICATION_EXTRA_CA_BASE64:-}" ]; then
  printf '%s' "$NOTIFICATION_EXTRA_CA_BASE64" | base64 -d > /tmp/notification-extra-ca.crt
  cp "$JAVA_HOME/lib/security/cacerts" /tmp/notification-cacerts
  keytool -importcert -noprompt -trustcacerts \
    -alias notification-extra-ca \
    -file /tmp/notification-extra-ca.crt \
    -keystore /tmp/notification-cacerts \
    -storepass changeit >/dev/null
  export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Djavax.net.ssl.trustStore=/tmp/notification-cacerts -Djavax.net.ssl.trustStorePassword=changeit"
fi

exec java -jar /app/app.jar
