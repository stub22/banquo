package com.appstract.banquo.roach

import org.postgresql.ds.PGSimpleDataSource

import java.sql.{PreparedStatement, ResultSet, ResultSetMetaData, Connection => SQL_Conn}
import java.util.UUID
import javax.sql.DataSource

object RunRoachTrial {
	def main(args: Array[String]): Unit = {
		println("RunRoachTrial: BEGIN")
		runTrial
		println("RunRoachTrial: END")
	}

	def runTrial() : Unit = {
		val pgds = PGDataSources.makePGDS
		val tp = new TrialPrelim {}
		tp.pingDB(pgds)
		tp.go(pgds)
	}

}
object PGDataSources {
	val flg_use99 = false // For when we are outside the docker-space, looking in.
	val flg_useEnvTxtUrl = false
	val hostName = "localhost" // localhost is the default
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
		implicit val conn: SQL_Conn = pgds.getConnection
		conn.setAutoCommit(false)
		schema.createTablesAsNeeded(conn)
		conn.commit()
		val baw = new BankAccountXactWriter{}
		val z1 = baw.makeAccount("Milton Friedman", "123 Main St, Anytown USA", BigDecimal("100.0"))
		val z2 = baw.makeAccount("John Keynes", "456 Andover St, Liverpool UK",  BigDecimal("200.0"))

		conn.close()
	}
}

