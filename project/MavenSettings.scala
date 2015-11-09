import sbt._

import scala.sys.SystemProperties

object MavenSettings {

  lazy val props = new SystemProperties()

  lazy val defaultSettingsFile = Path.userHome / ".m2"  / "settings.xml"
  lazy val mvnSettingsFile = Path(props.get("mavenSettings").getOrElse(defaultSettingsFile.absolutePath)).asFile
  lazy val mvnSettingsProfile = props.get("mavenSettingsProfile").getOrElse("comcast")
  lazy val mvnSnapshotRepoId = props.get("mavenSnapshotRepoId").getOrElse("cimrepo-snapshots")
  lazy val mvnReleaseRepoId = props.get("mavenReleaseRepoId").getOrElse("cimrepo")
  lazy val mvnRealm = props.get("mavenRealm").getOrElse("Sonatype Nexus Repository Manager")
  lazy val mvnSnapshotUrlOverride:Option[String] = props.get("mavenSnapshotUrlOverride")
  lazy val mvnReleaseUrlOverride:Option[String] = props.get("mavenReleaseUrlOverride")
  lazy val mvnPublishSnapshots = props.get("mavenPublishSnapshots").map(_.toBoolean).getOrElse(false) // by default, we don't publish snapshots

  lazy val mvnSettingsXml = xml.XML.loadFile(mvnSettingsFile)

  lazy val mvnSettingsProfileXml = for {
    prof <- mvnSettingsXml \ "profiles" \ "profile"
    id = (prof \ "id").text
    if id == mvnSettingsProfile
  } yield prof

  def loadRepositoryUrl(repoId:String, version:String):Option[String] = {

    if (isSnapshot(version) && mvnSnapshotUrlOverride.isDefined) {
      return mvnSnapshotUrlOverride
    } else if (mvnReleaseUrlOverride.isDefined) {
      return mvnReleaseUrlOverride
    }

    val possibilities = for {
      node <- mvnSettingsProfileXml
      repository <- node \ "repositories" \ "repository"
      id = (repository \ "id").text
      if id == repoId
    } yield (repository \ "url").text

    if (possibilities.isEmpty) {
      println(s"\r\n!!! UNABLE TO FIND REPOSITORY IN SETTINGS FOR REPOSITORY ID '$repoId'")
    } else {
      println(s"Found url $possibilities for repo '$repoId'")
    }

    possibilities.headOption
  }

  def mvnResolver(version:String):Option[Resolver] = {

    if (isSnapshot(version) && !mvnPublishSnapshots) {
      Some(Resolver.mavenLocal)
    } else {
      val resolver = mvnEffectiveRepoId(version) map {
        repoId =>
          loadRepositoryUrl(repoId, version) match {
            case Some(repoUrl: String) => Resolver.url(repoId, new URL(repoUrl))(Patterns(true, Resolver.mavenStyleBasePattern))
            case None => Resolver.mavenLocal // use maven local if we cannot find
          }
      }
      resolver.orElse(Some(Resolver.mavenLocal))
    }
  }

  lazy val mvnReleaseUrl:Option[String] = loadRepositoryUrl(mvnReleaseRepoId, "VER RELEASE")
  lazy val mvnSnapshotUrl:Option[String] = loadRepositoryUrl(mvnSnapshotRepoId, "VER SNAPSHOT")

  def mvnEffectiveRepoUrl(version:String):Option[String] = version match {
    case x if isSnapshot(x) => mvnSnapshotUrlOverride.orElse(mvnSnapshotUrl)
    case _ => mvnReleaseUrlOverride.orElse(mvnReleaseUrl)
  }

  def mvnEffectiveRepoId(version:String):Option[String] = version match {
    case x if isSnapshot(x) => Option(mvnSnapshotRepoId)
    case _ => Option(mvnReleaseRepoId)
  }

  def lookupCredentials(serverId:String, remoteUrl:String):Option[Credentials] = {
    println(s"\r\n!! $serverId; $remoteUrl")
    val host = new URI(remoteUrl).getHost
    val creds = for {
      s <- mvnSettingsXml \ "servers" \ "server"
      id = (s \ "id").text
      if id == serverId
      username = (s \ "username").text
      password = (s \ "password").text
    } yield Credentials(mvnRealm, host, username, password)

    creds.headOption
  }

  def mvnCredentials(version:String):Seq[Credentials] = {

    if (isSnapshot(version) && !mvnPublishSnapshots) {
      Seq.empty
    } else {
      val creds = for {
        s <- mvnSettingsXml \ "servers" \ "server"
        effectiveRepoId <- mvnEffectiveRepoId(version)
        effectiveRepoUrl <- mvnEffectiveRepoUrl(version)
        host = new URI(effectiveRepoUrl).getHost
        id = (s \ "id").text
        if id == effectiveRepoId
        username = (s \ "username").text
        password = (s \ "password").text
      } yield Credentials(mvnRealm, host, username, password)

      println("repo found " + mvnSettingsXml \ "profiles")

      for (s <- mvnSettingsXml \ "servers" \ "server") {
        println("server found " + s \ "id")
      }

      if (creds.length > 0) {
        creds.foreach{
          cred =>
            val direct = cred.asInstanceOf[DirectCredentials]
            println(s"Found credentials for host '${direct.host}', user '${direct.userName}', realm '${direct.realm}'")
        }
      }
      creds.toSeq
    }
  }

  def isSnapshot(version:String) = version.trim.endsWith("SNAPSHOT")
}