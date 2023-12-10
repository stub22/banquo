package com.appstract.banquo.main

import com.appstract.banquo.api.roach.DbConn
import com.appstract.banquo.impl.bank.{BankAccountReadOpsImpl, BankAccountWriteOpsImpl}
import com.appstract.banquo.impl.roach.RoachDbConnLayers
import com.appstract.banquo.svc.BanquoHttpAppBuilder
import zio.{RIO, Task, URIO, ZIO, ZIOAppDefault}
import zio.http.Server

object RunBanquoHttpApp extends ZIOAppDefault {

	// TODO:  Read our HTTP port number from .env
	val SERVER_PORT_NUM: Int = 8484

	val flg_useConfiguredConn = true

	override def run: Task[Any] = {
		val writeOps = new BankAccountWriteOpsImpl
		val readOps = new BankAccountReadOpsImpl

		val dbConnLayer = if (flg_useConfiguredConn)
			RoachDbConnLayers.dbcLayerConfigured
		else RoachDbConnLayers.dbcLayerDefault

		// Each HTTP request handler should acquire a fresh JDBC connection from the dbConnLayer.
		val httpAppBuilder = new BanquoHttpAppBuilder(writeOps, readOps, dbConnLayer)
		val httpApp = httpAppBuilder.makeHttpApp

		val appServiceNeedsServer: URIO[Server, Nothing] = Server.serve(httpApp.withDefaultErrorResponse)

		val httpServer = Server.defaultWithPort(SERVER_PORT_NUM)

		val serverApp = appServiceNeedsServer.provide(httpServer)

		// FIXME:  Table creation should probably be a deployment step.
		// Currently this job will eagerly try to connect to the database during our service initialization.
		// If that were to fail (e.g. because the database is not ready), then our tables would not be created,
		// and all subsequent operations would fail.
		val dbSetupJobNeedsDbLayer = setupDatabaseIfNeeded
		val dbSetup = dbSetupJobNeedsDbLayer.provideLayer(dbConnLayer).debug("RunBanquoHttpApp.dbSetup complete")

		// ZIO fiber runtime will run dbSetup, THEN our serverApp
		dbSetup *> serverApp
	}
	def setupDatabaseIfNeeded : RIO[DbConn, Unit] = {
		val dbSetupJob: ZIO[DbConn, Throwable, Unit] = RunRoachTests.setupSchema
		dbSetupJob
	}
}
