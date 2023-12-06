package com.appstract.banquo.roach

import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

import java.sql.{PreparedStatement, ResultSet, ResultSetMetaData, Connection => SQL_Conn}
import javax.sql.DataSource

object RunRoachZio extends ZIOAppDefault {
	override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {

		val dummyDbOp: ZIO[DbConn, Nothing, Unit] = ZIO.serviceWith[DbConn](dbc => {
			doThingWithConn(dbc)
		}).debug("zioThatNeedsDbConn debug")

		val tableCreateOp = setup.debug("schema setup")

		val comboOp = tableCreateOp //  dummyDbOp *> tableCreateOp

		val appToRun = comboOp.provideLayer(DbConnLayers.dbcLayer01)
		appToRun.debug("appToRun debug")
	}

	val myDirectSqlExec = new DirectSqlExecutor
	def doThingWithConn(dbc : DbConn) = {
		val sqlConn = dbc.sqlConn
		myDirectSqlExec.runSome("DROP TABLE dummy_dumdum")(dbc.sqlConn)
	}

	val mySqlExec = new SqlExecutor
	val schema = RoachSchema
	def setup = {
		val schemaCreate = schema.createTablesAsNeeded
		// Empiricially, it seems a commit is required for Couchbase to absorb DDL statements.
		val comm = mySqlExec.execCommit.debug(".execCommit")
		schemaCreate *> comm
	}

}



/**
 * https://jdbc.postgresql.org/documentation/datasource/
 *
 * org.postgresql.ds.PGSimpleDataSource   - no connection pooling
 * org.postgresql.ds.PGPoolingDataSource - basic pooling, has limits and flaws as discussed in doc
 */


