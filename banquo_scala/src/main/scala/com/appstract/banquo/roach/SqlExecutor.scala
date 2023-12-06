package com.appstract.banquo.roach

import zio.{Task, ZIO}

import java.sql.{PreparedStatement, ResultSet, ResultSetMetaData}
import scala.collection.mutable.ArrayBuffer

case class DbError(info : String)

class SqlExecutor {
	/***
	 * Uses mutable data features of the PreparedStatement and ResultSet.
	 */
	def execSqlAndPullRows[Row](prepStmtTxt : String, params: Seq[Any], rowGrabber : Function[ResultSet, Row]): ZIO[DbConn, Throwable, Seq[Row]] = {
		ZIO.log(s"execSqlAndPullRows called with stmt=${prepStmtTxt} and params: ${params}") *>
		ZIO.serviceWithZIO[DbConn](dbc => {
			ZIO.attemptBlocking {
				// Build/find the prepared SQL statement, which may be cached by the DB + JDBC
				val pstmt: PreparedStatement = dbc.sqlConn.prepareStatement(prepStmtTxt)

				// Apply the params to the prepared statement.  (These steps mutate the pstmt).
				params.zipWithIndex.foreach(pair => {
					val (paramVal, paramIdx) = pair
					applyParamAtZeroBasedIndex(pstmt, paramIdx, paramVal)
				})

				// Synchronously execute the query and capture its results inside of pstmt.
				val execRsltFlag: Boolean = pstmt.execute()
				// execRsltFlag is true when the SQL statement returns results.
				val rowSeq: Seq[Row] = if (execRsltFlag) {
					val rs: ResultSet = pstmt.getResultSet
					val rsmeta: ResultSetMetaData = rs.getMetaData
					val colCount: Int = rsmeta.getColumnCount
					val rsltBuf = new ArrayBuffer[Row]
					while (rs.next()) {
						val row = rowGrabber(rs)
						rsltBuf.addOne(row)
					}
					rs.close()
					rsltBuf.toSeq
				} else {
					throw new Exception("execSqlAndPullRows: PreparedStatement.execute returned false, but we expected a ResultSet.")
				}
				rowSeq
			}
		}).debug(".execSqlAndPullRows result")
	}
	def execSqlAndPullOneRow[Row](prepStmtTxt : String, params: Seq[Any], rowGrabber : Function[ResultSet, Row]) = {
		val execJob = execSqlAndPullRows(prepStmtTxt, params, rowGrabber)
		execJob.map(rowSeq => {
			assert(rowSeq.size == 1)
			rowSeq.head
		})
	}
	def execSqlAndPullOneString(prepStmtTxt : String, params: Seq[Any]): ZIO[DbConn, Throwable, String] = {
		val puller = (rs : ResultSet) => (rs.getString(1)) // JDBC columns use 1-based indexing
		execSqlAndPullOneRow[(String)](prepStmtTxt, params, puller)
	}
	def execSqlAndPullOneLong(prepStmtTxt: String, params: Seq[Any]): ZIO[DbConn, Throwable, Long] = {
		val puller = (rs: ResultSet) => (rs.getLong(1)) // JDBC columns use 1-based indexing
		execSqlAndPullOneRow[(Long)](prepStmtTxt, params, puller)
	}
	private def applyParamAtZeroBasedIndex(pstmt : PreparedStatement, pidx : Int, pval : Any) = {
		// JDBC PreparedStatement parameters start at index 1.
		val oneBasedIndex = pidx + 1
		pval match {
			case ps : String => pstmt.setString(oneBasedIndex, ps)
			case pl : Long => pstmt.setLong(oneBasedIndex, pl)
			case pi : Int => pstmt.setInt(oneBasedIndex, pi)
			case pbd : BigDecimal => pstmt.setBigDecimal(oneBasedIndex, pbd.bigDecimal)
		}
	}

	def execUpdateNoResult(prepStmtTxt: String): ZIO[DbConn, Throwable, Int] = {
		ZIO.serviceWithZIO[DbConn](dbc => {
			ZIO.attemptBlocking {
				val pstmt: PreparedStatement = dbc.sqlConn.prepareStatement(prepStmtTxt)
				val execRsltFlag: Boolean = pstmt.execute()
				val updateCount = if (!execRsltFlag) {
					pstmt.getUpdateCount
				} else {
					throw new Exception("execUpdateNoResult: PreparedStatement.execute returned true, but we expected false.")
				}
				updateCount
			}
		})
	}
	def execCommit(): ZIO[DbConn, Throwable, Unit] = {
		ZIO.serviceWithZIO[DbConn](dbc => {
			ZIO.attemptBlocking {
				dbc.sqlConn.commit()
			}
		})
	}
}
