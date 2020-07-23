FROM navikt/java:12
ENV JAVA_OPTS="-XX:-OmitStackTraceInFastThrow -Xms768m -Xmx1024m"

COPY cacert/GoogleTrustServices.pem /etc/ssl/certs/GoogleTrustServices.pem
COPY build/libs/pam-ad-statistics-demo-*.jar app.jar
EXPOSE 8080
