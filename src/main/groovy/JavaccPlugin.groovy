package com.smokejumperit.gradle

import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.api.plugins.*
import org.gradle.api.tasks.compile.Compile

class JavaccPlugin extends SjitPlugin {

  void apply(Project project) {
    project.apply(plugin:'java')
    project.apply(plugin:EnvPlugin)

    if(!project.hasProperty('javaccSrcDir')) project.javaccSrcDir = new File(project.projectDir, "src/javacc")
    if(project.javaccSrcDir instanceof File) project.javaccSrcDir = project.javaccSrcDir.absolutePath
    project.javaccSrcDir = project.javaccSrcDir.toString()

    [
      JavaccSrcDir: 'javaccSrcDir'
    ].each { taskName, propName ->
      project.task("make${taskName}") {
        description = "Makes the directory for '${propName}'"
        doLast {
          def dir = new File(project."$propName")
          if(!dir.exists()) dir.mkdirs()
          project."$propName" = dir.toString()
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
        include: ["**/*.jjt"]
      )
      outputs.files project.files(inputs.files.files.collect { 
        new File(it.parentFile, it.name - ".jjt" + ".jj")
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
        include: ["**/*.jj"]
      ))
      def allInputs = project.tasks.generateFromJJTree.outputs.files + inputs.files
      outputs.files allInputs.files*.parent.unique().collect({ parent ->
        project.fileTree(
          dir: parent,
          include: ["*.java"]
        )
      })

      doLast {
        allInputs.files.collect {
          it.name.endsWith(".jjt") ? 
            new File(it.parentFile, it.name - ".jjt" + ".jj") : it
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
      description = "Deletes *.jj and *.java adjacent to *.jjt and *.java adjacent to *.jj"
      ant.delete(verbose:true, quiet:false) {
        new File(project.javaccSrcDir).eachFileRecurse { file ->
          if(file.name.endsWith(".jjt")) {
            fileset(dir:file.parent, includes:'*.jj')
            fileset(dir:file.parent, includes:'*.java')
          } else if(file.name.endsWith(".jj")) {
            fileset(dir:file.parent, includes:'*.java')
          }
        }
      }
    }
    project.tasks.clean.dependsOn project.tasks.deleteJavaccGen
    
    project.task('compileJavacc', type:Compile, dependsOn:[
      project.tasks.generateFromJavacc, project.tasks.makeJavaccSrcDir
    ]) {
      description = "Compiles the files in the javaccSrcDir"
      source = project.javaccSrcDir
      destinationDir = new File(project.buildDir, "classes-javacc")
      classpath = project.files(destinationDir)
      doLast {
        def myClassesRoot = destinationDir
        def myClassesFiles = project.files(myClassesRoot)
        project.sourceSets.each { ss ->
          ss.compileClasspath = ss.compileClasspath + myClassesFiles
        }
        project.jar {
          from myClassesRoot
        }
      }
    }

    project.tasks.compileJava.dependsOn project.tasks.compileJavacc

  }

}
