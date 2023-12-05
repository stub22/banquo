package com.appstract.banquo.roach

import zio.ZIO

import java.sql.{PreparedStatement, ResultSet, ResultSetMetaData}
import scala.collection.mutable.ArrayBuffer

class SqlExecutor {
	def execSqlAndPullRows[Row](prepStmtTxt : String, rowGrabber : Function[ResultSet, Row]): ZIO[DbConn, Throwable, Seq[Row]] = {
		ZIO.serviceWith[DbConn](dbc => {
			val pstmt: PreparedStatement = dbc.sqlConn.prepareStatement(prepStmtTxt)
			val execRsltFlag: Boolean = pstmt.execute()

			val rowSeq: Seq[Row] = if (execRsltFlag) {
				val rs: ResultSet = pstmt.getResultSet
				val rsmeta: ResultSetMetaData = rs.getMetaData
				val colCount: Int = rsmeta.getColumnCount
				val rsltBuf = new  ArrayBuffer[Row]
				while (rs.next()) {
					val row = rowGrabber(rs)
					rsltBuf.addOne(row)
				}
				rsltBuf.toSeq
			} else {
				throw new Exception("execSqlAndPullRows: PreparedStatement.execute returned false, but we expected a ResultSet.")
			}
			rowSeq
		})
	}

	def execUpdateNoResult(prepStmtTxt: String): ZIO[DbConn, Throwable, Int] = {
		ZIO.serviceWith[DbConn](dbc => {
			val pstmt: PreparedStatement = dbc.sqlConn.prepareStatement(prepStmtTxt)
			val execRsltFlag: Boolean = pstmt.execute()
			val updateCount = if (!execRsltFlag) {
				pstmt.getUpdateCount
			} else {
				throw new Exception("execUpdateNoResult: PreparedStatement.execute returned true, but we expected false.")
			}
			updateCount
		})
	}
}

trait ResultExtractor {

}