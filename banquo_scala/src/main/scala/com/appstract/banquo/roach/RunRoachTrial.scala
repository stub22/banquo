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
		val info = pgds.getDescription
		val generatedDriverUrl = pgds.getURL
		println(s"Pgds data source info: [${info}]\ndriverURL=${generatedDriverUrl}")
		val conn: SQL_Conn = pgds.getConnection
		println(s"Got conn: ${conn}")
		conn.setAutoCommit(false)
		val uid01 = UUID.randomUUID();
		println(s"Made uid01: ${uid01.toString} ") // with t-stamp: ${uid01.timestamp()}")
		val runnr = new SqlRunner{}
		val creTblTxt = "CREATE TABLE IF NOT EXISTS accounts (id UUID PRIMARY KEY, balance int8)"
		runnr.runSome(creTblTxt)(conn)
		conn.commit()

	}
}
trait SqlRunner {
	def runSome(prepStmtTxt : String)(sqlConn : SQL_Conn) = {
		val pstmt: PreparedStatement = sqlConn.prepareStatement(prepStmtTxt)
		val execRsltFlag: Boolean = pstmt.execute()
		val summTxt = if (execRsltFlag) {
			val rs: ResultSet = pstmt.getResultSet
			val rsmeta: ResultSetMetaData = rs.getMetaData
			val colCount: Int = rsmeta.getColumnCount
			s"Exec result colCnt=${colCount}, rsMeta=${rsmeta}"
		} else {
			val updtCnt = pstmt.getUpdateCount
			s"Exec update count: ${updtCnt}"
		}
		println("SqlRunner.runSome got: " + summTxt)
		// connection.commit();
		// connection.rollback();
	}
}
// driverURL=jdbc:postgresql://localhost:26257/defaultdb?ApplicationName=appy_RunRoachTrial_yay&ssl=false
/***
 * def execute: Boolean  @throws[SQLException]
 * Executes the SQL statement in this PreparedStatement object, which may be any kind of SQL statement.
 * Some prepared statements return multiple results; the execute method handles these complex statements as well as
 * the simpler form of statements handled by the methods executeQuery and executeUpdate.
 * The execute method returns a boolean to indicate the form of the first result. You must call either the method
 * getResultSet or getUpdateCount to retrieve the result; you must call getMoreResults to move to any subsequent result(s).
 * Returns:
 * true if the first result is a ResultSet object; false if the first result is an update count or there is no result
 * Throws:
 * SQLTimeoutException – when the driver has determined that the timeout value that was specified by the setQueryTimeout
 * method has been exceeded and has at least attempted to cancel the currently running Statement
 * SQLException – if a database access error occurs; this method is called on a closed PreparedStatement or an argument
 * is supplied to this method
 * See Also:
 * Statement.execute, Statement.getResultSet, Statement.getUpdateCount, Statement.getMoreResults
 */