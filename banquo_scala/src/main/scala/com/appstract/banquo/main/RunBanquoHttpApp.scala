package com.appstract.banquo.main

import com.appstract.banquo.api.DbConn
import com.appstract.banquo.impl.bank.{BankAccountReadOpsImpl, BankAccountWriteOpsImpl}
import com.appstract.banquo.impl.roach.RoachDbConnLayers
import com.appstract.banquo.svc.BanquoHttpAppBuilder
import zio.{URIO, ZIO, ZIOAppDefault}
import zio.http.Server

object RunBanquoHttpApp extends ZIOAppDefault {
	override def run = {
		val writeOps = new BankAccountWriteOpsImpl
		val readOps = new BankAccountReadOpsImpl

		// TODO: Read Roach-DB connection info from .env
		val dbConnLayer = RoachDbConnLayers.dbcLayer01

		// Each HTTP request handler should acquire a fresh JDBC connection from the dbConnLayer.
		val httpAppBuilder = new BanquoHttpAppBuilder(writeOps, readOps, dbConnLayer)
		val httpApp = httpAppBuilder.makeHttpApp

		val appServiceNeedsServer: URIO[Server, Nothing] = Server.serve(httpApp.withDefaultErrorResponse)

		// TODO:  Read our HTTP port number from .env
		val serverPortNum : Int = 8484
		val httpServer = Server.defaultWithPort(serverPortNum)

		val serverApp = appServiceNeedsServer.provide(httpServer)

		val dbSetupJobNeedsDbLayer = setupDatabaseIfNeeded
		val dbSetup = dbSetupJobNeedsDbLayer.provideLayer(dbConnLayer).debug("RunHttpApp.dbSetup complete")

		// ZIO fiber runtime will run dbSetup, THEN our serverApp
		dbSetup *> serverApp
	}
	def setupDatabaseIfNeeded = {
		val dbSetupJob: ZIO[DbConn, Throwable, Unit] = RunRoachTests.setup
		dbSetupJob
	}
}
