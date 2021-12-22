FROM cimg/openjdk:17.0 AS GRADLE_BUILD_IMAGE
USER circleci
ENV APP_HOME=/home/circleci/project
ENV GRADLE_USER_HOME=/home/circleci/gradle_cache
WORKDIR $APP_HOME
COPY --chown=circleci:circleci build.gradle.kts settings.gradle.kts gradle.properties gradlew $APP_HOME/
COPY --chown=circleci:circleci gradle $APP_HOME/gradle
RUN ./gradlew build -i -Dorg.gradle.daemon=false || true

FROM cimg/openjdk:17.0
USER circleci
ENV APP_HOME=/home/circleci/project
WORKDIR $APP_HOME
COPY --chown=circleci:circleci --from=GRADLE_BUILD_IMAGE /home/circleci/gradle_cache /home/circleci/.gradle
COPY --chown=circleci:circleci . .

ENV GOTENBERG_ENDPOINT_URL=http://gotenberg:3000
CMD ["/home/circleci/project/gradlew", "clean", "documentGenerationTest", "-Dorg.gradle.daemon=false"]
