package com.appstract.banquo.roach

import org.postgresql.ds.PGSimpleDataSource

import java.sql.{PreparedStatement, ResultSet, ResultSetMetaData, Connection => SQL_Conn}
import java.util.UUID

object RunRoachTrial {
	def main(args: Array[String]): Unit = {
		println("RunRoachTrial: BEGIN")
		runTrial
		println("RunRoachTrial: END")
	}

	val flg_use99 = false	// For when we are outside the docker-space, looking in.
	val flg_useEnvTxtUrl = false
	val hostName = "localhost" // localhost is the default
	val dbPortNum = if (flg_use99) 26299 else 26257
	val portNums : Array[Int] = Array(dbPortNum)
	val dbName = "defaultdb"
	val flag_useSSL = false
	val userName = "root"
	val copiedDriverURL = "jdbc:postgresql://localhost:26257/defaultdb?ApplicationName=appy_RunRoachTrial_yay&ssl=false"

	def runTrial() : Unit = {
		val appName = "appy_RunRoachTrial_yay"
		val jdbcEnvTxtURL : String = if (false)
			System.getenv("JDBC_DATABASE_URL")
		else 	copiedDriverURL // 	"jdbc:postgresql://root@localhost:26257/defaultdb?ssl=false"
		// this URL does not work - guess it wants the jdbc:  in front but some docs seemed to say for data-SOURCE...

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

		val tp = new TrialPrelim {}
		tp.pingDB(pgds)
		tp.go(pgds)
	}

}

trait TrialPrelim {
	def pingDB(pgds : PGSimpleDataSource): Unit = {
		val info = pgds.getDescription
		val generatedDriverUrl = pgds.getURL
		println(s"Pgds data source info: [${info}]\ndriverURL=${generatedDriverUrl}")
		val conn: SQL_Conn = pgds.getConnection
		println(s"Got conn: ${conn}")
		conn.setAutoCommit(false)
		val uid01 = UUID.randomUUID();
		println(s"Made uid01: ${uid01.toString} ") // with t-stamp: ${uid01.timestamp()}")
		conn.commit()
		conn.close()
	}
	val schema = RoachSchema
	def go(pgds : PGSimpleDataSource) = {
		val conn: SQL_Conn = pgds.getConnection
		schema.createTablesAsNeeded(conn)
		conn.close()
	}
}

