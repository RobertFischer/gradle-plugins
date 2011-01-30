package com.smokejumperit.gradle

import org.gradle.api.*
import org.gradle.api.plugins.*

class CukePlugin extends SjitPlugin {

	private interface Props {
		String JR_HOME = "jruby.home"
		String JR_CP = "jruby.classpath"
	}

	private static final String CUKE = "cuke"

  void apply(Project project) {
    def cuke4DukeVersion = "0.4.2"

    def pluginLogger = this.logger

    project.apply(plugin:ClassLoadersPlugin)
    project.apply(plugin:EnvPlugin)
    project.apply(plugin:ProjectExtPlugin)

    project.convention.plugins.cuke = new CukePluginConvention()
    project.convention.plugins.cuke.featuresDir = "${project.projectDir}/src/test/features"
    project.convention.plugins.cuke.configs = [CUKE]

    project.configurations { 
      cuke {
        extendsFrom(testRuntime, runtime)
      }
    }
    project.repositories {
      mavenRepo name:'cukes.info', urls:'http://cukes.info/maven/'
      mavenCentral name:"MavenCentral (via CukePlugin)"
    }
    project.dependencies {
      cuke "cuke4duke:cuke4duke:$cuke4DukeVersion"
    }

    def cukeConfigs = {->
      return project.convention.plugins.cuke.configs.collect { "$it" } as String[]
    }

    project.task("setJRubyHome") << {
      if(!project.ant.properties[Props.JR_HOME]) {
        project.ant.properties[Props.JR_HOME] = 
          System.properties[Props.JR_HOME] ?:  
          project.env.JR_HOME ?:
          new File(project.rootDir, ".gradle/.jruby").canonicalPath
      }   
      project.ant.mkdir(dir:project.ant.properties[Props.JR_HOME])
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
      project.ant.taskdef(name:CUKE, classname:'cuke4duke.ant.CucumberTask', 
        classpathref:Props.JR_CP
      )
    }
    project.cukeTaskdef.dependsOn project.setJRubyClasspath

    project.task('cukeGems') << {
      project.ant.taskdef(name:'gem', classname:'cuke4duke.ant.GemTask', 
        classpathref:Props.JR_CP
      )
      project.ant.gem(args:"install cuke4duke --version $cuke4DukeVersion --source http://gemcutter.org/")
      project.ant.gem(args:"install rspec --source http://gems.rubyforge.org/")
    }
    project.cukeGems.onlyIf { 
      def file = new File(project.ant.properties[Props.JR_HOME], "gems/cuke4duke-$cuke4DukeVersion")
      pluginLogger.info("$file exits? ${file.exists()}")
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
        project.ant.classpath {
					project.ant.pathelement(path:project.configurations.cuke.asPath)
					project.sourceSets.each { ss ->
						project.ant.pathelement(path:ss.compileClasspath.asPath)
						project.ant.pathelement(location:ss.classesDir.canonicalPath)
					}
				}
      }

    }
    project.runFeatures.dependsOn project.classes, project.testClasses, project.cukeGems, project.cukeTaskdef, project.setJRubyConfig

  }
}
