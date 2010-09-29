package com.smokejumperit.gradle

import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.api.plugins.*
import java.util.concurrent.atomic.AtomicBoolean
import org.gradle.api.artifacts.maven.*

class MavenProxyPlugin extends SjitPlugin {

  static final AtomicBoolean didInit = new AtomicBoolean(false)
  static volatile URL url
  
  void addRepository(Project project) {
    if(!url) {
      logger.error("NO URL FOR MAVEN PROXY FOUND")
    } else {
      String urlString = url.toString()
      logger.info("Adding local proxy repository: " + urlString)
      project.repositories.addFirst([
        name:"Local Maven Proxy", 
        url:urlString
      ])
    }
  }

	void registerRepoWithServer(repoUrl) {
		if(!url) {
			throw new IllegalStateException("Require Maven Proxy URL to be generated")
		}
		repoUrl = "/servlet/PutRepo?url=" + URLEncoder.encode("$repoUrl", "UTF-8")
		repoUrl = new URL(url, repoUrl)
		try {
			def is = repoUrl.openConnection()?.inputStream()
			logger.debug("Registered repo URL with Maven Proxy [$repoUrl]\n${is?.text}")
			is?.close()
		} catch (IOException ioe) {
			logger.warn("Error posting a repo URL to the Maven Proxy [$repoUrl]", ioe)
		}
	}

  void apply(Project project) {
		project.metaClass.addMavenRepo = { proxyUrl ->
			if(proxyUrl) {
				project.afterEvaluate {
					registerRepoWithServer(proxyUrl)
					project.repositories {
						mavenRepo urls:proxyUrl
					}
				}
			}
		}
		project.metaClass.setMavenRepos = { List proxyUrls ->
			proxyUrls?.unique()?.each { project.addMavenRepo(it) }
		}

    project.afterEvaluate {
      Properties props = loadProperties(project)

      if(!url) url = new URL("${props['serverName'] ?: ("127.0.0.1:" + props['port'])}/${props['prefix']}")

			// See if the server is running
      try {
				if(!didInit.compareAndSet(false, true))  {
					def conn = url.openConnection()
					conn.readTimeout = 100
					def b = conn.inputStream.read()
					if(b == -1) throw new IOException("No data to be read from repository root")
					logger.info("Assuming server is running: could open URL connection to " + url)
				}
      } catch(IOException ioe) {  
        logger.info("Assuming server is not yet running: failed to open URL connection to " + url)
        logger.debug("Error in connection to server at " + url + " (not yet started?) >> ${ioe.message}")
        startServer(props)
      } finally {
				addRepository(project)
			}
    }
  }

  void startServer(Properties props) {
    logger.debug("Starting server at " + url)

    def wkdir = new File(System.getProperty("user.home", "."), ".gradle/maven-proxy").absoluteFile
    logger.info("Starting server in " + wkdir)
    if(wkdir.mkdirs()) logger.debug("Created directory " + wkdir + " for Maven Proxy")
    
    if(props['repo.local.store']?.startsWith(".")) {
      def repoDir = new File(wkdir, props['repo.local.store']).absoluteFile
      if(repoDir.mkdirs()) logger.debug("Created directory " + repoDir + " for Maven Proxy repository")
    }

    logger.debug("Writing maven-proxy.properties")
    def propsFile = new File(wkdir, "maven-proxy.properties")
    if(propsFile.exists()) propsFile.text=""
    propsFile.withOutputStream { os -> props.store(os, "Maven-Proxy properties") }

    logger.debug("Writing maven-proxy-log4j.properties")
    def log4jFile = new File(wkdir, "maven-proxy-log4j.properties")
    if(!log4jFile.exists() && !props['reload-maven-proxy-log4j']) {
      this.getClass().getResource("maven-proxy-log4j.properties").withInputStream { is ->
        log4jFile.withOutputStream { os ->
          is.eachByte { b -> os.write(b) }
        }
      }
    }

    log.debug("Writing maven-proxy.jar")
    def jarFile = new File(wkdir, "maven-proxy.jar")
    if(!jarFile.exists() && !props['reload-maven-proxy-jar']) {
      this.getClass().getResource("maven-proxy.jar").withInputStream { is ->
        jarFile.withOutputStream { os -> 
          is.eachByte { b -> os.write(b) }
        }
      }
    }

    log.info("Starting the Maven-Proxy server in " + wkdir)
    def procArgs = [
      new File(System.getProperty("java.home", ""), "/bin/java").canonicalPath, 
      "-Xmx64m",
      "-Dlog4j.configuration=${log4jFile.absolutePath}",
      "-jar", jarFile.absolutePath, 
      propsFile.absolutePath
    ] 
    log.debug("Starting process with arguments: " + procArgs)
    ProcessBuilder pb = new ProcessBuilder(procArgs as String[])
    pb.directory(wkdir).redirectErrorStream(true).start()
  }

  Properties loadProperties(Project project) {
    Properties props = new Properties()
    if(project.hasProperty("mavenProxy.propertiesFile")) {
      File file = new File(project."mavenProxy.propertiesFile").absoluteFile
      log.info("Loading Maven-Proxy properties from "  + file)
      file.withInputStream { is -> props.load(is) }
    } else {
      log.info("Using default Maven-Proxy properties")
      this.getClass().getResource("maven-proxy.properties").withInputStream { is ->
        props.load(is)
      }
    }

    loadUrlProperties(props)
  
    return props
  }

  void loadUrlProperties(Properties props) {
    def urlFile = new File(System.getProperty("user.dir", "."), ".gradle/maven-proxy-urls.txt").absoluteFile
    if(urlFile.exists()) {
      log.info("Loading Maven-Proxy URL file at " + urlFile)
      urlFile.text.split("\\s+").unique().each { url ->
        log.debug("Adding url " + url)
        def urlName = url.replace("^http://", "").replace("[^\\w]+", "-")
        props["repo.${urlName}.url"] = url
        props["repo.${urlName}.cache.period"] = "7200"
        props["repo.list"] = props["repo.list"] + ",${urlName}"
      }
    } else {
      log.debug("No Maven-Proxy URL file found at " + urlFile + " (it's optional; don't panic!)")
    }
  }

}
