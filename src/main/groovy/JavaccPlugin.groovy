package com.smokejumperit.gradle

import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.api.plugins.*
import org.gradle.api.tasks.compile.Compile

class JavaccPlugin extends SjitPlugin {

	private interface Files {
		interface Ext {
			String JAVA = ".java"
			String JAVACC = ".jj"
			String JJTREE = ".jjt"
		}
		String ALL_JAVA = "*${Ext.JAVA}"
		String ALL_JAVACC = "*${Ext.JAVACC}"
		String ALL_JJTREE = "*${Ext.JJTREE}"
	}

	private interface Props {
		String JAVACC_SRC_DIR = "javaccSrcDir"
	}

  void apply(Project project) {
    project.apply(plugin:'java')
    project.apply(plugin:EnvPlugin)

    if(!project.hasProperty(Props.JAVACC_SRC_DIR)) {
			project."${Props.JAVACC_SRC_DIR}" = new File(project.projectDir, "src/javacc")
		}
    if(project."${Props.JAVACC_SRC_DIR}" instanceof File) {
			project."${Props.JAVACC_SRC_DIR}" = project.javaccSrcDir.absolutePath
		}
    project."${Props.JAVACC_SRC_DIR}" = project."${Props.JAVACC_SRC_DIR}".toString()

    def compileTo = new File(project.buildDir, "classes-javacc")

    [
      JavaccSrcDir: Props.JAVACC_SRC_DIR
    ].each { taskName, propName ->
      project.task("make${taskName}") {
        description = "Makes the directory for '${propName}'"
        onlyIf { 
          // May change in execution
          def dir = new File(project."$propName")
          !dir.exists() 
        }
        doLast { 
          // May change in execution
          def dir = new File(project."$propName")
          dir.mkdirs() 
        }
      }
    }

    project.task('ensureJavaccHome') {
      description = "Fails if JAVACC_HOME env variable is not set"
      if(!project.env['JAVACC_HOME']) {
        throw new StopExecutionException("Need the JAVACC_HOME environment variable to be specified")
      }
    }

    project.task('generateFromJJTree', dependsOn:[
      project.tasks.ensureJavaccHome, project.tasks.makeJavaccSrcDir
    ]) {
      description = "Makes JavaCC files from JJTree files"
      inputs.files project.fileTree(
        dir: project.javaccSrcDir,
        include: ["**/${Files.ALL_JJTREE}"]
      )
      outputs.files project.files(inputs.files.files.collect { 
        new File(it.parentFile, it.name - Files.Ext.JJTREE + Files.Ext.JAVACC)
      })
      doLast {
        inputs.files.files.each { file ->
          project.ant.jjtree(
            javaccHome:project.env.JAVACC_HOME,
            target:file.toString(),
            outputDirectory:file.parent
          )
        }
      }
    }

    project.task('generateFromJavacc', dependsOn:[
      project.tasks.ensureJavaccHome, project.tasks.makeJavaccSrcDir, 
      project.tasks.generateFromJJTree
    ]) {
      description = "Makes Java files from JavaCC files"
      inputs.files (project.tasks.generateFromJJTree.inputs.files + project.fileTree(
        dir: project.javaccSrcDir,
        include: ["**/${Files.ALL_JAVACC}"]
      ))
      def allInputs = project.tasks.generateFromJJTree.outputs.files + inputs.files
      outputs.files allInputs.files*.parent.unique().collect({ parent ->
        project.fileTree(
          dir: parent,
          include: [Files.ALL_JAVA]
        )
      }) 

      doLast {
        allInputs.files.collect {
          it.name.endsWith(Files.Ext.JJTREE) ? 
            new File(it.parentFile, it.name - Files.Ext.JJTREE + Files.Ext.JAVACC) : it
        }.unique().findAll { it.exists() }.each { file ->
          project.ant.javacc(
            javaccHome:project.env.JAVACC_HOME, 
            target:file.toString(),
            outputDirectory:file.parent
          )
        }
      }
    }

    project.task('deleteJavaccGen') << {
      description = "Deletes ${Files.ALL_JAVACC} and ${Files.ALL_JAVA} adjacent to ${Files.Ext.JJTREE}, " +
										"and ${Files.ALL_JAVA} adjacent to ${Files.ALL_JAVACC}"
      ant.delete(verbose:true, quiet:false) {
        new File(project.javaccSrcDir).eachFileRecurse { file ->
          if(file.name.endsWith(Files.Ext.JJTREE)) {
            fileset(dir:file.parent, includes:Files.ALL_JAVACC)
            fileset(dir:file.parent, includes:Files.ALL_JAVA)
          } else if(file.name.endsWith(Files.Ext.JAVACC)) {
            fileset(dir:file.parent, includes:Files.ALL_JAVA)
          }
        }
      }
    }
    project.tasks.clean.dependsOn project.tasks.deleteJavaccGen

		project.tasks.compileJava.dependsOn project.tasks.generateFromJavacc
   
    project.task('compileJavacc', type:Compile, dependsOn:[
      project.tasks.generateFromJavacc, project.tasks.makeJavaccSrcDir
    ]) {
      description = "Compiles the files in the javaccSrcDir"
      source = project.javaccSrcDir
      destinationDir = compileTo
      classpath = project.configurations.compile

      def myClassesRoot = destinationDir
      def addClasspath = { ss->
        ss.compileClasspath = ss.compileClasspath + project.files(myClassesRoot)
      }

      project.sourceSets.each(addClasspath)
      project.sourceSets.whenObjectAdded(addClasspath)
      project.jar {
        from myClassesRoot
      }
    }
    project.tasks.compileJava.dependsOn project.tasks.compileJavacc

  }

}
