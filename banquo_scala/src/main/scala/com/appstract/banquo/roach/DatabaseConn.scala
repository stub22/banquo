package com.appstract.banquo.roach

import zio.{Scope, ZIO, ZLayer}

import java.sql.{PreparedStatement, ResultSet, ResultSetMetaData, Connection => SQL_Conn}
import javax.sql.DataSource

case class DbConn(sqlConn: SQL_Conn)

trait DbConnOps {
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

object DbConnLayers {
	val myConnOps = new DbConnOps {}
	val dbcLayer01 = ZLayer.scoped(myConnOps.scopedConn(PGDataSources.makePGDS))
}