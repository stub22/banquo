# Use prebuilt image from https://github.com/sbt/docker-sbt for JDK11 + Scala 2.13 + SBT 1.9.7 
FROM sbtscala/scala-sbt:eclipse-temurin-focal-11.0.21_9_1.9.7_2.13.12

RUN mkdir /opt/japp
# .dockerignore should prevent any unwanted files from transferring (e.g. target/, .idea/)
COPY banquo_scala/ /opt/japp/banquo
WORKDIR /opt/japp/banquo

# USER sbtuser

CMD ["sbt", "clean", "run"]
# CMD ["ls","-R", "/opt/japp"]