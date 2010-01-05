package com.smokejumperit.gradle

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.apache.commons.lang.text.StrTokenizer as Toke
import org.apache.commons.lang.text.StrMatcher as Match

class RunJRubyPlugin extends SjitPlugin {
  void use(Project project, ProjectPluginsContainer projectPluginsHandler) { 
    def pluginLogger = this.logger

    project.usePlugin(ClassLoadersPlugin)
    project.usePlugin(EnvPlugin)
    project.usePlugin(ProjectExtPlugin)
    project.configurations { 
      jruby 
      jrubyPluginYaml
    }
    project.repositories {
      mavenCentral(name:"${this.class.simpleName}MavenCentralRepo")
      mavenRepo(name:"${this.class.simpleName}SnakeyYamlRepo", urls:"http://snakeyamlrepo.appspot.com/repository")
    }
    project.dependencies { 
      jruby "org.jruby.embed:jruby-embed:0.1.2" // TODO Update to a more recent JRuby
      jrubyPluginYaml "org.yaml:snakeyaml:1.5"
    }
    String[] defaultConfigs = ['jruby'] as String[]
    def splitCmd = { String cmd ->
      def toker = new Toke(cmd)
      toker.delimiterMatcher = Match.splitMatcher()
      toker.quoteMatcher = Match.quoteMatcher()
      return toker.tokenArray
    }

    def foundGemHome = {-> // Lazy thunk
      def gemHome = null
      return {->
        if(!gemHome) gemHome = findGemHome(project)
        return gemHome
      }
    }.call()
    project.metaClass.getGemHome = foundGemHome 
    project.metaClass.gemHome = { String gem -> 
      def dir = project.gemHome
      if(!dir.exists()) {
        pluginLogger.debug("$dir does not exist: cannot find gem $gem")
        return null
      }
      
      def gemDir = null
      String dirPrefix = "$gem-"
      dir.eachFileRecurse { 
        if(it.name =~ ('^' + gem + '-(?:\\.?\\d+)*$')) {
          if(!gemDir || it.name > gemDir.name) {
            gemDir = it
          }
        }
      }

     return gemDir == null ? null : project.tryRelativePath(gemDir)
    }
    project.metaClass.gemScript = { String gem ->
      def dir = project.gemHome(gem)
      if(!dir.exists()) {
        pluginLogger.debug("$dir does not exist: cannot find home dir for $gem")
      }
  
      dir = new File(dir, "bin")
      if(!dir.exists()) {
        pluginLogger.debug("$dir does not exist: cannot find bin dir for $gem")
      }

      return project.tryRelativePath(new File(dir, gem))
    }

    project.metaClass.runJRuby = { String cmdArg, String[] configs=defaultConfigs ->
      def cmdArray = splitCmd(cmdArg)
      pluginLogger.debug("JRuby Commands: ${cmdArray as List}")

      def JRuby = classFor('org.jruby.Main', configs)
      def curThread = Thread.currentThread()

      pluginLogger.info("Running JRuby: $cmdArg")
      curThread.setContextClassLoader(JRuby.class.classLoader)
      JRuby.main(cmdArray)
      curThread.setContextClassLoader(ClassLoader.systemClassLoader) // Allow JRuby to be GC'ed
      pluginLogger.debug("Done running JRuby: $cmdArg")

    }

    project.metaClass.useGem = { String gem ->
      if(!project.gemHome(gem)) {
        pluginLogger.info("Installing JRuby gem: $gem")
        project.runJRuby("-S gem install --no-rdoc --no-ri $gem")
        pluginLogger.debug("Done installing JRuby gem: $gem")
        if(!project.gemHome(gem)) throw new Exception("Could not install $gem")
      } 
    }

  }

  File findGemHome(project) {
    File gemHome = new File(System.properties['user.home'], '.gem')
    logger.debug("Looking for Gem Home based on $project: defaulting gem home to $gemHome")
    if(project.env.GEM_HOME) {
      gemHome = new File(project.env.GEM_HOME ?: "${gemHome}")
      logger.debug("Found environment variable GEM_HOME=${project.env.GEM_HOME}: gem home is now $gemHome")
    } 
    def homeGemRc = new File(System.properties['user.home'], '.gemrc')
    if(homeGemRc.exists()) {
      logger.debug("Found gem.rc at $homeGemRc")
      def Yaml = project.classFor("org.yaml.snakeyaml.Yaml", 'jrubyPluginYaml')
      homeGemRc.withInputStream { inStream ->
        Map gemrc = Yaml.load(inStream) as Map
        gemHome = new File(gemrc.gemhome ?: "${gemHome}")
      }
      logger.debug("Found gem.rc at $homeGemRc: gem home is now $gemHome")
    }
    return gemHome
  }

}
