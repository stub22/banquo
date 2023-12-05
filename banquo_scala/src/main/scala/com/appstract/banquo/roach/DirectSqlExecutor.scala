package com.appstract.banquo.roach

import java.sql.{PreparedStatement, ResultSet, ResultSetMetaData, Connection => SQL_Conn}

case class DbError(info : String)

class DirectSqlExecutor {
	def makeStmt(prepStmtTxt: String)(implicit sqlConn: SQL_Conn) = {
		sqlConn.prepareStatement(prepStmtTxt)
	}
	def runSome(prepStmtTxt: String)(implicit sqlConn: SQL_Conn) = {
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
	def runAndGetSingleStringResult(prepStmtTxt: String)(implicit sqlConn: SQL_Conn) = {
	}
}
// driverURL=jdbc:postgresql://localhost:26257/defaultdb?ApplicationName=appy_RunRoachTrial_yay&ssl=false
/** *
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