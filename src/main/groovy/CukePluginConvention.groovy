package com.smokejumperit.gradle

import org.gradle.api.*
import org.gradle.api.plugins.*

class CukePluginConvention {
	boolean warn = false
  String featuresDir
  //String stepsOutputDir
  List configs
	List gems
}
