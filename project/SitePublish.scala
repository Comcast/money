import java.io.FileInputStream
import java.net.URL

import com.github.sardine._
import sbt._

import scala.collection.JavaConversions._
import scala.sys.SystemProperties

object SitePublish {

  lazy val props = new SystemProperties()
  lazy val publishUrl = props.get("publishUrl").getOrElse("http://repo.dev.cim.comcast.net/sites")
  lazy val publishServerId = props.get("publishServerId").getOrElse("maven.site.upload")

  val publishWebDAV = TaskKey[Unit]("publish-webdav", "Publishes the specified directory to a webdav location")

  def webdav(remoteUrl:String, rootDirName:String, username:String, password:String, siteDir:File) = {

    val remoteTarget = remoteUrl + "/" + rootDirName
    val remoteURL = new URL(remoteTarget)

    val remoteBaseURL = new URL(remoteUrl)

    val sardine = SardineFactory.begin(username, password)
    sardine.enablePreemptiveAuthentication(remoteURL.getHost)

    def putDirectory(dir: File, remoteDir: String): Unit = {
      val baseURL = new URL(remoteDir)

      if (!sardine.exists(baseURL.toString)) {
        println(s"\r\nCreating remote directory $baseURL")
        sardine.createDirectory(baseURL.toString)
      }
      dir.listFiles.foreach {
        file =>
          if (file.isDirectory) {
            putDirectory(file, remoteDir + "/" + file.name)
          } else {
            val destURL = new URL(baseURL + "/" + file.name)
            println(s"\r\n\tputting file ${file.getParent + "/" + file.getName} on $destURL")
            sardine.put(destURL.toString, new FileInputStream(file))
          }
      }
    }

    // fail safe, so we don't accidentally for some reason destroy all of the sites, that would be sad
    if ((remoteTarget != "http://repo.dev.cim.comcast.net/sites") && (sardine.exists(remoteURL.toString))) {
      println(s"\r\n!!! Deleting target directory $remoteTarget on host ${remoteURL.getHost}")
      collectRemoteDir(remoteBaseURL, remoteURL, sardine).map {
        dir => dir.delete(sardine)
      }
    }
    putDirectory(siteDir, remoteTarget)
  }

  def collectRemoteDir(remoteSiteBase: URL, remoteSiteUrl:URL, sardine:Sardine): Option[RemoteDir] = {

    if (sardine.exists(remoteSiteUrl.toString)) {
      var subs:Seq[RemoteDir] = Seq.empty
      var files:Seq[URL] = Seq.empty
      val remoteResources = asScalaBuffer(sardine.list(remoteSiteUrl.toString, 1))
      remoteResources.foreach {
        res =>
          val resUrl = new URL(remoteSiteBase, res.getPath)
          if (res.isDirectory && (resUrl.toString != remoteSiteUrl.toString)) {
            collectRemoteDir(remoteSiteBase, resUrl, sardine).map {
              nestedDir =>
              subs = subs :+ nestedDir
            }
          } else if (!res.isDirectory) {
            files = files :+ resUrl
          }
      }
      Some(RemoteDir(remoteSiteUrl, files, subs))
    } else {
      println(s"$remoteSiteUrl doesnt exist")
      None
    }
  }
}

case class RemoteDir(url:URL, files:Seq[URL] = Seq.empty, subDirectories:Seq[RemoteDir] = Seq.empty) {

  def delete(sardine:Sardine):Unit = {
    subDirectories.foreach {
      sub =>
        println("SUBDIR: " + url.toString)
        sub.delete(sardine)
    }
    files.foreach {
      file =>
        println("FILE: " + file.toString)
        if (sardine.exists(file.toString)) {
          sardine.delete(file.toString)
        }
    }
    if (url.toString != "http://repo.dev.cim.comcast.net/sites"
        && url.toString != "http://repo.dev.cim.comcast.net/sites/"
        && sardine.exists(url.toString)) {
      println("DIR: " + url.toString)
      sardine.delete(url.toString)
    }
  }
}

