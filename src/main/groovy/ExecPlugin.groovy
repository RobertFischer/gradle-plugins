package com.smokejumperit.gradle

import org.gradle.api.*
import org.gradle.api.plugins.*

class ExecPlugin extends SjitPlugin {

  void apply(Project project) {
    project.metaClass.exec = { String cmd, String baseDir ->
      delegate.exec(cmd, new File(baseDir))
    }
    project.metaClass.exec = { String cmd, File baseDir=new File('.') ->
			project.execIn(baseDir, cmd)
		}
		project.metaClass.execIn = { File baseDir, Object... cmd ->
			project.execIn(baseDir, cmd*.toString() as String[])
		}
		project.metaClass.execIn = { File baseDir, String... cmd ->
      logger.info("Executing `${cmd.join(' ')}` (in $baseDir)")
      def proc = Runtime.runtime.exec(cmd, null, baseDir)

      logger.trace("Routing output and error streams for `$cmd`")
      proc.consumeProcessOutput(System.out, System.err)

      logger.debug("Closing input stream to `$cmd`")
      proc.outputStream.close() 

      logger.debug("Waiting on `$cmd`")
      def result = proc.waitFor()
      logger.info("Done executing `$cmd`")

      if(result) {
        throw new Exception("$cmd in $baseDir failed with non-zero result code $result")
      } else {
        logger.debug("Successful result from `$cmd`")
      }

    }
  }
}
