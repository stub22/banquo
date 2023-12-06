package com.appstract.banquo.main

import com.appstract.banquo.api.DbConn
import com.appstract.banquo.impl.bank.{BankAccountReadOpsImpl, BankAccountWriteOpsImpl}
import com.appstract.banquo.impl.roach.RoachDbConnLayers
import com.appstract.banquo.svc.BankAccountHttpAppBuilder
import zio.{URIO, ZIO, ZIOAppDefault}
import zio.http.Server

object RunBanquoHttpApp extends ZIOAppDefault {
	override def run = {
		val writeOps = new BankAccountWriteOpsImpl
		val readOps = new BankAccountReadOpsImpl
		val httpAppBuilder = new BankAccountHttpAppBuilder
		val httpApp = httpAppBuilder.makeHttpApp(writeOps, readOps)

		val appServiceNeedsLayers: URIO[Server with DbConn, Nothing] = Server.serve(httpApp.withDefaultErrorResponse)

		// TODO:  Read our HTTP port number from .env
		val serverPortNum : Int = 8484
		val httpServer = Server.defaultWithPort(serverPortNum)

		// TODO: Read Roach-DB connection info from .env
		val dbConnLayer = RoachDbConnLayers.dbcLayer01

		val serverApp = appServiceNeedsLayers.provide(httpServer, dbConnLayer)

		val dbSetupJobNeedsLayer = setupDatabaseIfNeeded
		val dbSetup = dbSetupJobNeedsLayer.provideLayer(dbConnLayer).debug("RunHttpApp.dbSetup complete")

		dbSetup *> serverApp
	}
	def setupDatabaseIfNeeded = {
		val dbSetupJob: ZIO[DbConn, Throwable, Unit] = RunRoachTests.setup
		dbSetupJob
	}
}
