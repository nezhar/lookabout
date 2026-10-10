# Android build toolchain for agents working on Lookabout. No FROM line:
# VibePod appends this to the agent's base image (see vibepod.dev/docs/overlays).
#
# Everything lives under /opt because $HOME is a host mount at runtime.

ARG CMDLINE_TOOLS=16111833
ARG ANDROID_PLATFORM=android-36
ARG BUILD_TOOLS=36.0.0

RUN apt-get update \
    && apt-get install -y --no-install-recommends unzip \
        $(apt-cache show openjdk-21-jdk-headless >/dev/null 2>&1 \
            && echo openjdk-21-jdk-headless || echo openjdk-17-jdk-headless) \
    && rm -rf /var/lib/apt/lists/* \
    && ln -s "$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")" /opt/jdk \
    # Writable, so java-proxy.sh can add the proxy CA as the agent user.
    && chmod a+w "$(readlink -f /opt/jdk/lib/security/cacerts)"

ENV JAVA_HOME=/opt/jdk \
    ANDROID_HOME=/opt/android-sdk \
    GRADLE_USER_HOME=/opt/gradle-home
ENV PATH="${JAVA_HOME}/bin:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"

ADD https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS}_latest.zip /tmp/cmdline-tools.zip
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools \
    && unzip -q /tmp/cmdline-tools.zip -d ${ANDROID_HOME}/cmdline-tools \
    && mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest \
    && rm /tmp/cmdline-tools.zip \
    && yes | sdkmanager --licenses >/dev/null \
    && sdkmanager "platforms;${ANDROID_PLATFORM}" "build-tools;${BUILD_TOOLS}" "platform-tools" >/dev/null \
    # The agent may not run as root; let Gradle and sdkmanager write here.
    && chmod -R a+rwX ${ANDROID_HOME} \
    && mkdir -p ${GRADLE_USER_HOME} && chmod 1777 ${GRADLE_USER_HOME}

COPY java-proxy.sh /opt/vibepod/java-proxy.sh
RUN chmod a+rx /opt/vibepod/java-proxy.sh
