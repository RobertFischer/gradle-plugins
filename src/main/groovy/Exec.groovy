package com.smokejumperit.gradle

import org.gradle.api.*
import org.gradle.api.plugins.*

class ExecPlugin implements Plugin {
  void use(Project project, ProjectPluginsContainer projectPluginsHandler) { 
    project.metaClass.exec = { String cmd, String baseDir ->
      delegate.exec(cmd, new File(baseDir))
    }
    project.metaClass.exec = { String cmd, File baseDir=new File('.') ->
      println "$cmd (in $baseDir)"

      def proc = Runtime.runtime.exec(cmd, null, baseDir)
      proc.consumeProcessOutput(System.out, System.err)
      proc.outputStream.close() // Actually closing process's stdin
      def result = proc.waitFor()

      if(result) {
        throw new Exception("$cmd failed with result code $result")
      }

    }
  }
}
