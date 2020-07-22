FROM navikt/java:12
ENV JAVA_OPTS="-XX:-OmitStackTraceInFastThrow -Xms768m -Xmx1024m"

COPY build/libs/pam-godkjenningsregistre-*.jar app.jar
EXPOSE 8080
