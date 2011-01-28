package com.smokejumperit.gradle

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.api.artifacts.*

class OneJarPlugin extends SjitPlugin {
	
	static final String jarName = "one-jar-ant-task-0.97.jar";

  void apply(Project project) {
		project.apply(plugin:'java')
		project.apply(plugin:ExecPlugin)

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

		project.task('typedefOneJar', dependsOn:root.tasks.unpackOneJar) {
			description = "Defines the one-jar task on ant"
			doFirst {
				ant.property(name:"one-jar.dist.dir", value:oneJarDir.absolutePath)
				ant.import(file:new File(oneJarDir, "one-jar-ant-task.xml").absolutePath, optional:false)
			}
		}

		project.task('oneJar', dependsOn:[project.tasks.jar, project.tasks.typedefOneJar]) {
			def jar = project.tasks.jar
			File jarFile = new File(jar.destinationDir, jar.archiveName - jar.extension - "." + "-oneJar." + jar.extension)
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
					project.sourceSets*.resources*.getSrcDirs()?.flatten()?.findAll { it?.exists() }?.each { resdir ->
						ant.fileset(dir:resdir.absolutePath)
					}
					ant.lib {
						runConf.findAll { !it.isDirectory() }.each { depFile ->
							ant.fileset(file:depFile.absolutePath)
						}
					}
				}

				Date date = new Date()
				String name = jar.baseName
				project.configurations.archives.addArtifact(
					[
						getClassifier: { -> "standalone" },
						getDate: {-> date },
						getExtension: {-> "jar" },
						getType: {-> "jar" },
						getFile: {-> jarFile },
						getName: {-> name }
					] as PublishArtifact
				)
			}
		}
/*
		project.task("execOneJar", dependsOn:[project.tasks.oneJar]) {
			doFirst {
				def workingDir = new File(new File(project.buildDir, "one-jar"), "exec")
				workingDir.mkdirs()
				def jarFile = project.tasks.oneJar.outputs.files.singleFile
				project.execIn(workingDir, 
					"${System.getProperty("java.home", "")}/bin/java", "-jar", jarFile.absolutePath
				)
			}
		}
*/
	}

	File writeOneJarManifestFile(jar) {
		def manifestFile = File.createTempFile("one-jar-manifest", "mf")
		manifestFile.withWriter { w -> 
			def m = jar.manifest.effectiveManifest
			String main = m.attributes.remove("Main-Class")
			if(main) m.attributes.put("One-Jar-Main-Class", main)
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
