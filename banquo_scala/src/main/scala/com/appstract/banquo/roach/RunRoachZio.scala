package com.appstract.banquo.roach

import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

import java.sql.{PreparedStatement, ResultSet, ResultSetMetaData, Connection => SQL_Conn}
import javax.sql.DataSource

object RunRoachZio extends ZIOAppDefault {
	override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {

		val zioThatNeedsDbConn: ZIO[DbConn, Nothing, Unit] = ZIO.serviceWith[DbConn](dbc => {
			doThingWithConn(dbc)
		})

		val appToRun = zioThatNeedsDbConn.provideLayer(DbConnLayers.dbcLayer01)
		appToRun
	}

	val mySqlExec = new SqlExecutor
	def doThingWithConn(dbc : DbConn) = {
		val sqlConn = dbc.sqlConn
		mySqlExec.runSome("DROP TABLE dummy_dumdum")(dbc.sqlConn)
	}
}
case class DbConn(sqlConn: SQL_Conn)
object DbConnLayers {
	val myConnOps = new ConnOps {}
	val dbcLayer01 = ZLayer.scoped(myConnOps.scopedConn(PGDataSources.makePGDS))
}

trait ConnOps {
	def openConn(dsrc: => DataSource) : ZIO[Any, Throwable, DbConn] = {
		ZIO.attemptBlocking {
			val conn = dsrc.getConnection
			conn.setAutoCommit(false)
			DbConn(conn)
		}.debug(".openConn")
	}
	def closeConn(dbConn : DbConn) : ZIO[Any, Throwable, Unit] = {
		ZIO.attemptBlocking {
			dbConn.sqlConn.close()
		}.debug(".closeConn")
	}
	def closeAndLogErrors(dbConn : DbConn) : ZIO[Any, Nothing, Unit] = {
		val closeEff = closeConn(dbConn)
		closeEff.orElse(ZIO.logError("Problem closing dbConn"))
	}
	def scopedConn(dsrc: => DataSource): ZIO[Scope, Throwable, DbConn] = {
		ZIO.acquireRelease (openConn(dsrc)) (closeAndLogErrors(_))
	}
}
/**
 * https://jdbc.postgresql.org/documentation/datasource/
 *
 * org.postgresql.ds.PGSimpleDataSource   - no connection pooling
 * org.postgresql.ds.PGPoolingDataSource - basic pooling, has limits and flaws as discussed in doc
 */


