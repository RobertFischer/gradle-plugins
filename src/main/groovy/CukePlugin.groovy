package com.smokejumperit.gradle

import org.gradle.api.*
import org.gradle.api.plugins.*

class CukePlugin extends SjitPlugin {
  void apply(Project project) {
    def cuke4DukeVersion = "0.3.2"

    def pluginLogger = this.logger

    project.apply(plugin:ClassLoadersPlugin)
    project.apply(plugin:EnvPlugin)
    project.apply(plugin:ProjectExtPlugin)

    project.convention.plugins.cuke = new CukePluginConvention()
    project.convention.plugins.cuke.featuresDir = "${project.projectDir}/src/test/features"
    project.convention.plugins.cuke.configs = ['cuke']

    project.configurations { 
      cuke {
        extendsFrom testRuntime, runtime
        ['test', 'main'].each { project.sourceSets."$it".classes }
      }
    }
    project.repositories {
      mavenRepo name:'cukes.info', urls:'http://cukes.info/maven/'
      mavenCentral name:"MavenCentral (via CukePlugin)"
    }
    project.dependencies {
      cuke "cuke4duke:cuke4duke:$cuke4DukeVersion"
/*
      cuke 'org.jruby:jruby-complete:1.4.0'
      cuke 'org.picocontainer:picocontainer:2.10.2'
      cuke 'junit:junit:4.8.1'
*/
    }

    def cukeConfigs = {->
      return project.convention.plugins.cuke.configs.collect { "$it" } as String[]
    }

    project.task("setJRubyHome") << {
      if(!project.ant.properties['jruby.home']) {
        project.ant.properties['jruby.home'] = 
          System.properties['jruby.home'] ?:  
          project.env.JRUBY_HOME ?:
          new File(project.rootDir, ".gradle/.jruby").canonicalPath
      }   
      project.ant.mkdir(dir:project.ant.properties['jruby.home'])
    }   

    project.task("setJRubyClasspath") << {
      def configs = cukeConfigs()
      pluginLogger.debug("Configurations: $configs")
      project.ant.path(id:'jruby.classpath') {
        project.classPathElementsFor(configs).each {
          pluginLogger.debug("\tPath element:\t$it")
          pathelement(location:it)
        }   
      }
    }

    project.task("setJRubyConfig") << {}
    project.setJRubyConfig.dependsOn project.setJRubyHome, project.setJRubyClasspath

    project.task('cukeTaskdef') << {
      project.ant.taskdef(name:'cuke', classname:'cuke4duke.ant.CucumberTask', 
        classpathref:'jruby.classpath'
      )
    }
    project.cukeTaskdef.dependsOn project.setJRubyClasspath

    project.task('cukeGems') << {
      project.ant.taskdef(name:'gem', classname:'cuke4duke.ant.GemTask', 
        classpathref:'jruby.classpath'
      )
      project.ant.gem(args:"install cuke4duke --version $cuke4DukeVersion --source http://gemcutter.org/")
      project.ant.gem(args:"install rspec --source http://gems.rubyforge.org/")
    }
    project.cukeGems.onlyIf { 
      def file = new File(project.ant.properties['jruby.home'], "gems/cuke4duke-$cuke4DukeVersion-java")
      println "$file exits? ${file.exists()}"
      return !file.exists()
    }
    project.cukeGems.dependsOn project.cukeTaskdef, project.setJRubyConfig

    project.task('runFeatures') << { 
      description = "Runs the Cucumber features"

      def featuresDir = project.tryRelativePath(project.convention.plugins.cuke.featuresDir)
      pluginLogger.debug("Features directory: $featuresDir")

      def configs = cukeConfigs()
      pluginLogger.debug("Configurations: $configs")

      def outputDir = project.tryRelativePath(new File(project.buildDir, "cuke-output"))
      project.ant.mkdir(dir:outputDir)
      // --require target/test-classes -- Do we really need that?
      project.ant.cuke(args:"--verbose --color --format pretty --format junit --out $outputDir $featuresDir") {
        classpath(refid:'jruby.classpath')
      }

    }
    project.runFeatures.dependsOn project.classes, project.testClasses, project.cukeGems, project.cukeTaskdef, project.setJRubyConfig

  }
}
