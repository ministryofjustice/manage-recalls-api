FROM cimg/openjdk:16.0

USER root
RUN apt-get update

USER circleci
COPY --chown=circleci:circleci . /home/circleci/project
WORKDIR /home/circleci/project

ENV SPRING_PROFILES_ACTIVE=int-test
CMD ["/home/circleci/project/gradlew", "clean", "documentGenerationTest", "-Dorg.gradle.daemon=false"]


