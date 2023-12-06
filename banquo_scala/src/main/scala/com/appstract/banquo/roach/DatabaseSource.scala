package com.appstract.banquo.roach

import org.postgresql.ds.PGSimpleDataSource

/**
 * https://jdbc.postgresql.org/documentation/datasource/
 *
 * org.postgresql.ds.PGSimpleDataSource   - no connection pooling
 * org.postgresql.ds.PGPoolingDataSource - basic pooling, has limits and flaws as discussed in doc
 */

object PGDataSources {
	val flg_use99 = false // For when we are outside the docker-space, looking in.
	val flg_useEnvTxtUrl = false
	val hostName_UNUSED = "localhost" // localhost is the default used by PGSimpleDataSource.
	val dbPortNum = if (flg_use99) 26299 else 26257
	val portNums: Array[Int] = Array(dbPortNum)
	val dbName = "defaultdb"
	val flag_useSSL = false
	val userName = "root"
	val copiedDriverURL = "jdbc:postgresql://localhost:26257/defaultdb?ApplicationName=appy_RunRoachTrial_yay&ssl=false"

	def makePGDS : PGSimpleDataSource = {
		val appName = "appy_RunRoachTrial_yay"
		val jdbcEnvTxtURL: String = if (false)
			System.getenv("JDBC_DATABASE_URL")
		else copiedDriverURL // 	"jdbc:postgresql://root@localhost:26257/defaultdb?ssl=false"

		val pgds = new PGSimpleDataSource()
		pgds.setApplicationName(appName);
		if (flg_useEnvTxtUrl)
			pgds.setUrl(jdbcEnvTxtURL)
		else {
			pgds.setPortNumbers(portNums)
			pgds.setSsl(flag_useSSL)
			pgds.setUser(userName)
			pgds.setDatabaseName(dbName)
		}
		pgds
	}
}