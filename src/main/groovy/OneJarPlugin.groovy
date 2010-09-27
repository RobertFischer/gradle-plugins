package com.smokejumperit.gradle

import org.gradle.api.*
import org.gradle.api.plugins.*

class OneJarPlugin extends SjitPlugin {
	
	static final String jarName = "one-jar-ant-task-0.97.jar";

  void apply(Project project) {
		if(!project.getTasksByName('jar', false)) project.apply(plugin:Java)

		final oneJarDir = new File(project.rootProject.buildDir, "one-jar")

		def root = project.rootProject

		if(!root.getTasksByName('unpackOneJar', false)) {
			root.task('unpackOneJar') {
				description = "Unpacks the one-jar distribution"
				onlyIf { !oneJarDir.exists() }
				doFirst {
					oneJarDir.mkdirs()
					def jarFile = writeOutJar(oneJarDir)
					ant.unzip(
						src:jarFile.absolutePath,
						dest:oneJarDir.absolutePath,
						failOnEmptyArchive: true
					)
				}
			}
		}

		if(!root.getTasksByName('typedefOneJar', false)) {
			root.task('typedefOneJar', dependsOn:root.tasks.unpackOneJar) {
				description = "Defines the one-jar task on ant"
				doFirst {
					ant.property(name:"one-jar.dist.dir", value:oneJarDir.absolutePath)
					ant.import(file:new File(oneJarDir, "one-jar-ant-task.xml").absolutePath, optional:false)
				}
			}
		}

		project.task('makeOneJar', dependsOn:[project.tasks.jar, root.tasks.typedefOneJar]) {
			def jar = project.tasks.jar
			File jarFile = new File(jar.destinationDir, jar.archiveName - jar.extension - "." + "-all." + jar.extension)
			description = "Makes the fat jar file"
			inputs.files jar.outputs.files
			outputs.files jarFile
			doFirst {
				def runConf = project.configurations.runtime
				def manifestFile = writeOneJarManifestFile(jar) 
				ant.'one-jar'(destFile:jarFile.absolutePath, manifest:manifestFile.absolutePath) {
					ant.main(jar:jar.archivePath.absolutePath) {
						runConf.findAll { it.isDirectory() }.each { depDir ->
							ant.fileset(dir:depDir.absolutePath)
						}
					}
					ant.lib {
						runConf.findAll { !it.isDirectory() }.each { depFile ->
							ant.fileset(file:depFile.absolutePath)
						}
					}
				}
			}
		}
	}

	File writeOneJarManifestFile(jar) {
		def manifestFile = File.createTempFile("one-jar-manifest", "mf")
		manifestFile.withWriter { w -> 
			def m = jar.manifest.effectiveManifest
			m.attributes.put("One-Jar-Main-Class", m.attributes.remove("Main-Class"))
			m.writeTo(w)
		}
		manifestFile.deleteOnExit()
		return manifestFile
	}

	File writeOutJar(File dir) {
		def jarFile = new File(dir, jarName)
		jarFile.delete()
		jarFile.withOutputStream { os ->
			this.class.getResourceAsStream("/${jarName}").eachByte { b ->
				os.write(b)
			}
		}
		return jarFile
	}

}
