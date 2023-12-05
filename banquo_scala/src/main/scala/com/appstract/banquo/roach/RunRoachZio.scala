package com.appstract.banquo.roach

import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

import java.sql.{PreparedStatement, ResultSet, ResultSetMetaData, Connection => SQL_Conn}
import javax.sql.DataSource

object RunRoachZio extends ZIOAppDefault {
	override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {

		val zioThatNeedsDbConn: ZIO[DbConn, Nothing, Unit] = ZIO.serviceWith[DbConn](dbc => {
			doThingWithConn(dbc)
		}).debug("zioThatNeedsDbConn debug")

		val sqlExec = new SqlExecutor
		val zzz = sqlExec

		val appToRun = zioThatNeedsDbConn.provideLayer(DbConnLayers.dbcLayer01)
		appToRun.debug("appToRun debug")
	}

	val myDirectSqlExec = new DirectSqlExecutor
	def doThingWithConn(dbc : DbConn) = {
		val sqlConn = dbc.sqlConn
		myDirectSqlExec.runSome("DROP TABLE dummy_dumdum")(dbc.sqlConn)
	}

	val mySqlExec = new SqlExecutor


}



/**
 * https://jdbc.postgresql.org/documentation/datasource/
 *
 * org.postgresql.ds.PGSimpleDataSource   - no connection pooling
 * org.postgresql.ds.PGPoolingDataSource - basic pooling, has limits and flaws as discussed in doc
 */


