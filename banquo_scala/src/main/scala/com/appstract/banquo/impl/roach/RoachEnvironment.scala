package com.appstract.banquo.impl.roach

import zio.{IO, System, UIO, ZIO}

trait RoachEnvironment {
	val ROACH_HOST_ENV_NAME = "ROACH_HOST"
	val FALLBACK_HOST = "localhost"
	val ROACH_PORT_ENV_NAME = "ROACH_PORT"

	val FALLBACK_PORT_NUM : Int = 26257

	def getRoachHost : UIO[String] = {
		getEnvString(ROACH_HOST_ENV_NAME, FALLBACK_HOST)
	}

	def getRoachPort: UIO[Int] = {
		getEnvString(ROACH_PORT_ENV_NAME, FALLBACK_PORT_NUM.toString).map(_.toIntOption.getOrElse(FALLBACK_PORT_NUM))
	}
	def getEnvString(envVarName : String, fallback : String) : UIO[String] = {
		val envWithDebug: ZIO[Any, SecurityException, Option[String]] =
			System.env(envVarName).debug(s"System.env(${envVarName})")
		envWithDebug.orElseSucceed(Some(fallback)).map(_.getOrElse(fallback))
	}






}
