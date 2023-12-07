package com.appstract.banquo.impl.roach

import com.appstract.banquo.api.roach.{DbConn, DbEmptyResult, DbOtherError, DbProblem}
import com.appstract.banquo.api.roach.DbOpResultTypes.DbOpResult
import zio.{RIO, Task, URIO, ZIO}

import java.sql.{PreparedStatement, ResultSet, ResultSetMetaData}
import scala.collection.mutable.ArrayBuffer

/***
 * All these execXyz methods produce ZIO effects with Throwable error type.
 * Note that RIO[DbConn, X] is just an alias for ZIO[DbConn, Throwable, X].
 */
class SqlEffectMaker {
	/***
	 * Caller must supply a rowGrabber function that will be applied to the ResultSet on each row.
	 * The rowGrabber may call any of the .getXyz methods to pull the current row's fields.
	 * However rowGrabber should not call .next() or any other ResultSet cursor-control methods.
	 * TODO: Give the rowGrabber a more restricted fetching interface, that can only pull values from current row.
	 *
	 * This implementation uses mutable data features of the PreparedStatement and ResultSet.
	 * It also assumes that all result row-sets fit easily in memory!
	 *
	 * Note that execSqlAndPullRows may produce any error as a Throwable, as shown in the result type.
	 */

	def execSqlAndPullRows[Row](prepStmtTxt : String, params: Seq[Any], rowGrabber : Function[ResultSet, Row]):
							ZIO[DbConn, Throwable, Seq[Row]] = {
		ZIO.log(s"execSqlAndPullRows called with stmt=${prepStmtTxt} and params: ${params}") *>
		ZIO.serviceWithZIO[DbConn](dbc => {
			ZIO.attemptBlocking {
				// Build/find the prepared SQL statement, which may be cached by the DB + JDBC.
				val pstmt: PreparedStatement = dbc.getSqlConn.prepareStatement(prepStmtTxt)

				// Apply the params to the prepared statement.  (These steps mutate the pstmt).
				params.zipWithIndex.foreach(pair => {
					val (paramVal, paramIdx) = pair
					applyParamAtZeroBasedIndex(pstmt, paramIdx, paramVal)
				})

				// Synchronously execute the query, capturing result info inside of pstmt.
				val execRsltFlag: Boolean = pstmt.execute()
				// execRsltFlag is true when the SQL statement returns results.
				val rowSeq: Seq[Row] = if (execRsltFlag) {
					val rs: ResultSet = pstmt.getResultSet
					// val rsmeta: ResultSetMetaData = rs.getMetaData
					// val colCount: Int = rsmeta.getColumnCount
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

	/*
	All errors are handled, so the ZIO error type is Nothing.
	type URIO[-R, +A] = ZIO[R, Nothing, A]
	 */
	def execSqlAndPullOneRow[Row](prepStmtTxt: String, params: Seq[Any], rowGrabber: Function[ResultSet, Row]):
			URIO[DbConn, DbOpResult[Row]] = {
		val OP_NAME = "execSqlAndPullOneRow"
		val execJob = execSqlAndPullRows(prepStmtTxt, params, rowGrabber)
		val jobWithSizeHandled: ZIO[DbConn, Throwable, Either[DbProblem, Row]] = execJob.map(rowSeq => {
			val rowsetSize = rowSeq.size
			rowsetSize match {
				case 1 => Right(rowSeq.head)
				case 0 => Left(DbEmptyResult(OP_NAME, prepStmtTxt, params.mkString(", ")))
				case _ => Left(DbOtherError(OP_NAME, prepStmtTxt, params.mkString(", "), s"Expected 1 result but got ${rowsetSize}"))
			}
		})
		jobWithSizeHandled.catchAll(thrown =>
			ZIO.succeed(Left(DbOtherError(OP_NAME, prepStmtTxt, params.mkString(", "), thrown.toString))))
	}
	def execSqlAndPullOneString(prepStmtTxt : String, params: Seq[Any]): URIO[DbConn, DbOpResult[String]] = {
		val puller = (rs : ResultSet) => (rs.getString(1)) // JDBC columns use 1-based indexing
		execSqlAndPullOneRow[(String)](prepStmtTxt, params, puller)
	}
	def execSqlAndPullOneLong(prepStmtTxt: String, params: Seq[Any]): URIO[DbConn, DbOpResult[Long]] = {
		val puller = (rs: ResultSet) => (rs.getLong(1)) // JDBC columns use 1-based indexing
		execSqlAndPullOneRow[(Long)](prepStmtTxt, params, puller)
	}
	private def applyParamAtZeroBasedIndex(pstmt : PreparedStatement, pidx : Int, pval : Any): Unit = {
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
				val pstmt: PreparedStatement = dbc.getSqlConn.prepareStatement(prepStmtTxt)
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
				dbc.getSqlConn.commit()
			}
		})
	}
}
