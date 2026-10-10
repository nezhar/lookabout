#!/bin/sh
# Lets Java (Gradle, sdkmanager) use the VibePod proxy: trust its CA and route
# through HTTPS_PROXY. Both are per container, so this runs on every start.
set -e

CA=/etc/vibepod-proxy-ca/mitmproxy-ca-cert.pem
if [ -f "$CA" ]; then
    "$JAVA_HOME/bin/keytool" -delete -noprompt -cacerts -storepass changeit \
        -alias vibepod-proxy >/dev/null 2>&1 || true
    "$JAVA_HOME/bin/keytool" -importcert -noprompt -cacerts -storepass changeit \
        -alias vibepod-proxy -file "$CA" >/dev/null 2>&1
fi

mkdir -p "$GRADLE_USER_HOME"
props="$GRADLE_USER_HOME/gradle.properties"
: > "$props"
proxy="${HTTPS_PROXY:-$https_proxy}"
if [ -n "$proxy" ]; then
    # http://user:pass@host:port
    rest="${proxy#*://}"
    rest="${rest%%/*}"
    case "$rest" in
        *@*) creds="${rest%@*}"; hostport="${rest##*@}" ;;
        *) creds=""; hostport="$rest" ;;
    esac
    host="${hostport%:*}"
    port="${hostport##*:}"
    for scheme in http https; do
        echo "systemProp.$scheme.proxyHost=$host" >> "$props"
        echo "systemProp.$scheme.proxyPort=$port" >> "$props"
        echo "systemProp.$scheme.nonProxyHosts=localhost|127.0.0.1" >> "$props"
        if [ -n "$creds" ]; then
            echo "systemProp.$scheme.proxyUser=${creds%%:*}" >> "$props"
            echo "systemProp.$scheme.proxyPassword=${creds#*:}" >> "$props"
        fi
    done
    # Java disables Basic auth for HTTPS tunnels unless told otherwise.
    echo "systemProp.jdk.http.auth.tunneling.disabledSchemes=" >> "$props"
fi
