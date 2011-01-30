package com.smokejumperit.gradle

import org.gradle.api.*
import org.gradle.api.plugins.*

class BiteScriptPlugin extends SjitPlugin {

	private interface TaskName {	
		String CREATE_BS_DIR = "createBiteScriptDir"
		String DL_BS = "downloadBiteScript"
		String UNPACK_BS = "unpackBiteScript"
		String INSTALL_BS = "installBiteScript"
	}

  void apply(Project project) {

    project.apply(plugin:RunJRubyPlugin)
    project.apply(plugin:ProjectExtPlugin)

		def root = project.rootProject
		final File biteScriptUrl = new URL("http://github.com/headius/bitescript/tarball/master")
		final File biteScriptDir = new File(new File(root.projectDir, ".gradle"), "bite_script")
		final File biteScriptZip = new File(biteScriptDir, "biteScript.zip")
		//final File biteScriptBin = new File(biteScriptDir, "bin")

		if(!root.getTasksByName(TaskName.CREATE_BS_DIR, false)) {
			root.task(TaskName.CREATE_BS_DIR, type:Directory) {
				description = "Creates the BiteScript installation directory at ${project.tryRelativePath(biteScriptDir)}"
				dir = biteScriptDir
			}
		}

		if(!root.getTasksByName(TaskName.DL_BS, false)) {
			root.task(TaskName.DL_BS, dependsOn:[root.tasks.createBiteScriptDir]) {
				description = "Downloads BiteScript from ${biteScriptUrl}"
				onlyIf { !biteScriptZip.exists() }
				doFirst {
					biteScriptZip.bytes = biteScriptUrl.bytes
				}
			}
		}

		if(!root.getTasksByName(TaskName.UNPACK_BS, false)) {
			root.task(TaskName.UNPACK_BS, type:Copy, dependsOn:[root.tasks.createBiteScriptDir, root.tasks.downloadBiteScript]) {
				description = "Unpacks the downloaded bitescript"
				from root.zipTree(biteScriptZip)
				to root.biteScriptDir
			}
		}

		if(!root.getTasksByName(TaskName.INSTALL_BS, false)) {
			root.task(TaskName.INSTALL_BS, dependsOn:[root.tasks.unpackBiteScript]) {
				description = "Downloads BiteScript, installs it, and sets BiteScript Home"
				// TODO Finish implementing this
			}
		}

  }
}
