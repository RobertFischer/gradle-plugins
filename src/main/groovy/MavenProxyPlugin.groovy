package com.smokejumperit.gradle

import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.api.plugins.*
import java.util.concurrent.atomic.AtomicBoolean

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

  void apply(Project project) {
    project.afterEvaluate {
      if(!didInit.compareAndSet(false, true))  {
        addRepository(project)
        return
      }
 
      Properties props = loadProperties(project)
      
      url = new URL("${props['serverName']}/${props['prefix']}")
      try {
        def conn = url.openConnection()
        conn.readTimeout = 100
        def b = conn.inputStream.read()
        if(b == -1) throw new IOException("No data to be read from repository root")
      } catch(IOException ioe) {  
        logger.info("Assuming server is not yet running: failed to open URL connection to " + url)
        logger.debug("Error in connection to server at " + url + " (not yet started?)", ioe)
        startServer(props)
        addRepository(project)
        return
      }

      logger.info("Assuming server is running: could open URL connection to " + url)
    }
  }

  void startServer(Properties props) {
    logger.debug("Starting server at " + url)

    def wkdir = new File(System.getProperty("user.dir", "."), ".gradle/maven-proxy").absoluteFile
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
    ProcessBuilder pb = new ProcessBuilder([
      new File(System.getProperty("java.home", ""), "/bin/java").canonicalPath, 
      "-Xmx64m",
      "-Dlog4j.configuration=${log4jFile.absolutePath}",
      "-jar", jarFile.absolutePath, 
      propsFile.absolutePath
    ] as String[])
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
