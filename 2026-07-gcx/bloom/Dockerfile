FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
ARG MODULE
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -q -B -pl ${MODULE} -am package -DskipTests

FROM eclipse-temurin:21-jre

ARG OTEL_AGENT_VERSION=v2.15.0
ARG PYROSCOPE_OTEL_VERSION=v2.0.5
ARG PYROSCOPE_AGENT_VERSION=v2.5.1
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar /opt/otel/opentelemetry-javaagent.jar
ADD https://github.com/grafana/otel-profiling-java/releases/download/${PYROSCOPE_OTEL_VERSION}/pyroscope-otel-javaagent-extension.jar /opt/otel/pyroscope-otel-javaagent-extension.jar
ADD https://github.com/grafana/pyroscope-java/releases/download/${PYROSCOPE_AGENT_VERSION}/pyroscope.jar /opt/otel/pyroscope.jar

ENV OTEL_JAVAAGENT_EXTENSIONS=/opt/otel/pyroscope-otel-javaagent-extension.jar

ARG MODULE
COPY --from=build /src/${MODULE}/target/${MODULE}.jar /opt/app/app.jar

# The Pyroscope agent starts the profiler (and exposes the labels API to app
# code on the system classpath); the OTel extension only correlates spans.
ENTRYPOINT ["java", \
    "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints", \
    "-javaagent:/opt/otel/pyroscope.jar", \
    "-javaagent:/opt/otel/opentelemetry-javaagent.jar", \
    "-jar", "/opt/app/app.jar"]
