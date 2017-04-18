import com.typesafe.sbt.SbtGhPages.GhPagesKeys._
import microsites.ExtraMdFileConfig

lazy val root = Project("elastic4s", file("."))
  .settings(publish := {})
  .settings(publishArtifact := false)
  .settings(name := "elastic4s")
  .settings(mappings in(Compile, packageSrc) ++= {
    val base = (sourceManaged in Compile).value
    val files = (managedSources in Compile).value
    files.map { f => (f, f.relativeTo(base).get.getPath) }
  })
  .aggregate(
    core,
    http,
    embedded,
    tests,
    testkit,
    circe,
    jackson,
    json4s,
    playjson,
    sprayjson
  )

lazy val core = Project("elastic4s-core", file("elastic4s-core"))
  .settings(name := "elastic4s-core")
  .settings(libraryDependencies ++= Seq(
    "org.locationtech.spatial4j"    % "spatial4j"     % "0.6",
    "com.vividsolutions"            % "jts"           % "1.13",
    "com.fasterxml.jackson.core"    % "jackson-core"            % JacksonVersion        % "test",
    "com.fasterxml.jackson.core"    % "jackson-databind"        % JacksonVersion        % "test",
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"   % JacksonVersion        % "test" exclude("org.scala-lang", "scala-library")
  ))

lazy val http = Project("elastic4s-http", file("elastic4s-http"))
  .settings(name := "elastic4s-http")
    .settings(libraryDependencies += "org.elasticsearch.client" % "rest" % ElasticsearchVersion)
  .dependsOn(core)

lazy val embedded = Project("elastic4s-embedded", file("elastic4s-embedded"))
  .settings(
    name := "elastic4s-embedded",
    libraryDependencies ++= Seq(
      "org.elasticsearch"                     % "elasticsearch"             % ElasticsearchVersion,
      "org.elasticsearch.client"              % "transport"                 % ElasticsearchVersion,
      "com.fasterxml.jackson.dataformat"      % "jackson-dataformat-smile"  % JacksonVersion,
      "com.fasterxml.jackson.dataformat"      % "jackson-dataformat-cbor"   % JacksonVersion
    )
  )
  .dependsOn()

lazy val testkit = Project("elastic4s-testkit", file("elastic4s-testkit"))
  .settings(
    name := "elastic4s-testkit",
    libraryDependencies ++= Seq(
      "org.scalatest"                         %% "scalatest"                % ScalatestVersion,
      "com.fasterxml.jackson.core"            % "jackson-core" % JacksonVersion,
      "com.fasterxml.jackson.core"            % "jackson-databind" % JacksonVersion,
      "com.fasterxml.jackson.module"          %% "jackson-module-scala" % JacksonVersion exclude("org.scala-lang", "scala-library"),
      "com.fasterxml.jackson.datatype"        % "jackson-datatype-joda" % JacksonVersion,
      "org.apache.logging.log4j"              % "log4j-api"                 % Log4jVersion,
      "org.apache.logging.log4j"              % "log4j-core"                % Log4jVersion,
      "org.apache.logging.log4j"              % "log4j-1.2-api"             % Log4jVersion
    )
  )
  .dependsOn(core, embedded, http)

lazy val tests = Project("elastic4s-tests", file("elastic4s-tests"))
  .settings(
    name := "elastic4s-tests",
    libraryDependencies ++= Seq(
      "commons-io"                    % "commons-io"              % CommonsIoVersion      % "test",
      "org.mockito"                   % "mockito-all"             % MockitoVersion        % "test",
      "com.fasterxml.jackson.core"    % "jackson-core"            % JacksonVersion        % "test",
      "com.fasterxml.jackson.core"    % "jackson-databind"        % JacksonVersion        % "test",
      "com.fasterxml.jackson.module"  %% "jackson-module-scala"   % JacksonVersion        % "test" exclude("org.scala-lang", "scala-library"),
      "org.apache.logging.log4j"      % "log4j-api"               % "2.7"                 % "test",
      "org.apache.logging.log4j"      % "log4j-slf4j-impl"        % "2.7"                 % "test"
    )
  )
  .dependsOn(http, jackson, testkit % "test")

lazy val jackson = Project("elastic4s-jackson", file("elastic4s-jackson"))
  .settings(
    name := "elastic4s-jackson",
    libraryDependencies += "com.fasterxml.jackson.core"       % "jackson-core" % JacksonVersion,
    libraryDependencies += "com.fasterxml.jackson.core"       % "jackson-databind" % JacksonVersion,
    libraryDependencies += "com.fasterxml.jackson.module"     %% "jackson-module-scala" % JacksonVersion exclude("org.scala-lang", "scala-library"),
    libraryDependencies += "com.fasterxml.jackson.datatype"   % "jackson-datatype-joda" % JacksonVersion,
    libraryDependencies += "org.apache.logging.log4j"         % "log4j-api"             % Log4jVersion,
    libraryDependencies += "org.apache.logging.log4j"         % "log4j-core"            % Log4jVersion,
    libraryDependencies += "org.apache.logging.log4j"         % "log4j-1.2-api"         % Log4jVersion,
    libraryDependencies += "org.apache.logging.log4j"         % "log4j-api"             % "2.7"                 % "test",
    libraryDependencies += "org.apache.logging.log4j"         % "log4j-slf4j-impl"      % "2.7"                 % "test"
  ).dependsOn(core, testkit % "test")

lazy val circe = Project("elastic4s-circe", file("elastic4s-circe"))
  .settings(
    name := "elastic4s-circe",
    libraryDependencies += "io.circe" %% "circe-core"     % CirceVersion,
    libraryDependencies += "io.circe" %% "circe-generic"  % CirceVersion,
    libraryDependencies += "io.circe" %% "circe-parser"   % CirceVersion
  ).dependsOn(core, testkit % "test")

lazy val json4s = Project("elastic4s-json4s", file("elastic4s-json4s"))
  .settings(
    name := "elastic4s-json4s",
    libraryDependencies += "org.json4s" %% "json4s-core"    % Json4sVersion,
    libraryDependencies += "org.json4s" %% "json4s-jackson" % Json4sVersion
  ).dependsOn(core, testkit % "test")

lazy val playjson = Project("elastic4s-play-json", file("elastic4s-play-json"))
  .settings(
    name := "elastic4s-play-json",
    libraryDependencies += "com.typesafe.play" %% "play-json" % PlayJsonVersion
  ).dependsOn(core, testkit % "test")

lazy val sprayjson = Project("elastic4s-spray-json", file("elastic4s-spray-json"))
  .settings(
    name := "elastic4s-spray-json",
    libraryDependencies += "io.spray" %% "spray-json" % SprayJsonVersion
  ).dependsOn(core, testkit % "test")

lazy val docsMappingsAPIDir = settingKey[String]("Name of subdirectory in site target directory for api docs")

lazy val docs = project
  .in(file("docs"))
  .enablePlugins(MicrositesPlugin, ScalaUnidocPlugin)
  .settings(ghpages.settings)
  .settings(noPublishSettings)
  .settings(
    micrositeName := "Elastic4s",
    micrositeDescription := "Elasticsearch Scala Client",
    micrositeAuthor := "Stephen Samuel",
    micrositeHomepage := "https://sksamuel.github.io/elastic4s",
    micrositeGithubOwner := "sksamuel",
    micrositeGithubRepo := "elastic4s",
    micrositeBaseUrl := "/elastic4s",
    micrositeDocumentationUrl := "docs",
    micrositeTwitter := "",
    micrositeHighlightTheme := "atom-one-light",
    micrositeExtraMdFiles := Map(file("README.md") -> ExtraMdFileConfig("index.md", "home")),
    micrositePalette := Map(
      "brand-primary" -> "#729B79",
      "brand-secondary" -> "#2E2C2F",
      "brand-tertiary" -> "#2B2D42",
      "gray-dark" -> "#646767",
      "gray" -> "#475B63",
      "gray-light" -> "#8D99AE",
      "gray-lighter" -> "#EAF2E6",
      "white-color" -> "#FFFFFF"
    ),
    git.remoteRepo := "git@github.com:sksamuel/elastic4s.git",
    autoAPIMappings := true,
    docsMappingsAPIDir := "api",
    addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), docsMappingsAPIDir),
    ghpagesNoJekyll := false,
    fork in tut := true,
    fork in (ScalaUnidoc, unidoc) := true,
    includeFilter in makeSite := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.swf" | "*.yml" | "*.md",
    // push microsite on release
    releaseProcess += releaseStepTask(publishMicrosite)
  ).dependsOn(core, embedded, http, circe)

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)
