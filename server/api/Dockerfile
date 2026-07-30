FROM openjdk:8-jdk-alpine
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
COPY wisestar.mv.db wisestar.mv.db 
RUN mkdir /wisestar
VOLUME /files
VOLUME /logs
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app.jar ${0} ${@}"]