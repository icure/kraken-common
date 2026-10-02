FROM gradle:8.5.0-jdk21 as builder
ARG repoUsername
ARG repoPassword
ENV ORG_GRADLE_PROJECT_repoUsername=$repoUsername
ENV ORG_GRADLE_PROJECT_repoPassword=$repoPassword
ENV ORG_GRADLE_PROJECT_mavenRepository=https://maven.taktik.be/content/groups/public
ENV ORG_GRADLE_PROJECT_mavenReleasesRepository=https://maven.taktik.be/content/repositories/releases/
ENV ORG_GRADLE_PROJECT_mavenSnapshotsRepository=https://maven.taktik.be/content/repositories/snapshots/

WORKDIR /build
COPY . ./

RUN mv ci.settings.kts settings.gradle.kts

# git.version is written by the CI before the docker build and exposed to Gradle as the gitVersion property
RUN test -s git.version || (echo "git.version is missing or empty" && exit 1)

RUN export ORG_GRADLE_PROJECT_gitVersion="$(cat git.version)" && gradle -x test :dto:publish :domain:publish :utils:publish