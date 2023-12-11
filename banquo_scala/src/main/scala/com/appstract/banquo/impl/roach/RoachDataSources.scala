package com.appstract.banquo.impl.roach

import org.postgresql.ds.PGSimpleDataSource
import zio.{Task, ZIO}

/**
 * We use the PGSimpleDataSource, which does not provide any connection pooling.
 *
 * https://jdbc.postgresql.org/documentation/datasource/
 *
 * org.postgresql.ds.PGSimpleDataSource  - no connection pooling
 * org.postgresql.ds.PGPoolingDataSource - basic pooling; has limits and flaws as discussed in doc linked above.
 ***************************************************************************************
 * The default ports used by Cockroach DB are: 26257 for the DB, 8080 for the built in web console.
 * We use these default ports when testing Cockroach DB on the bare host OS (i.e. without Docker).
 *
 * When we launch Cockroach DB in a Docker container, we have set it to expose ports
 * 26299 and 8199 to the host OS. It is possible to connect to that instance from
 * the host network, or from another container in the bridge network.  See compose.yaml.
 */

object RoachDataSources {

	val flag_useSSL = false

	val dbName = "defaultdb"
	val userName = "root"

	val appName = "BanquoRoachTrial"

	/** Uses only hardcoded default parameters for DB connection.
	 */
	def makeDefaultPGDataSource : PGSimpleDataSource = {
		// localhost is the default used by PGSimpleDataSource.
		val hostName_UNUSED = "localhost"
		val flg_use99 = false
		val dbPortNum = if (flg_use99) 26299 else 26257
		val portNums: Array[Int] = Array(dbPortNum)

		val pgds = new PGSimpleDataSource()

		pgds.setApplicationName(appName)

		pgds.setPortNumbers(portNums)
		pgds.setSsl(flag_useSSL)
		pgds.setUser(userName)
		pgds.setDatabaseName(dbName)

		pgds
	}

	/** Reads environment variables to get parameters for DB connection.
	 */
	def  makeConfiguredPGDataSourceEffect : Task[PGSimpleDataSource] = {
		val myRoachEnv = new RoachEnvironment {}
		for {
			roachHost <- myRoachEnv.getRoachHost
			roachPort <- myRoachEnv.getRoachPort
			serverNames = Array[String](roachHost)
			portNums =  Array[Int](roachPort)
			pgds = new PGSimpleDataSource()
			_ = pgds.setApplicationName(appName)
			_ = pgds.setServerNames(serverNames)
			_ = pgds.setPortNumbers(portNums)
			_ = pgds.setSsl(flag_useSSL)
			_ = pgds.setUser(userName)
			_ = pgds.setDatabaseName(dbName)
		} yield(pgds)
	}
}