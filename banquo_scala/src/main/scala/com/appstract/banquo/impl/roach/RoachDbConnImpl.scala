package com.appstract.banquo.impl.roach

import com.appstract.banquo.api.roach.DbConn
import org.postgresql.ds.common.BaseDataSource
import zio.{Scope, ZIO, ZLayer}

import java.sql.{PreparedStatement, ResultSet, ResultSetMetaData, Connection => SQL_Conn}
import javax.sql.DataSource

private class RoachDbConnImpl(sqlConn: SQL_Conn) extends DbConn {
	override def getSqlConn: SQL_Conn = sqlConn
}

trait RoachDbConnOps {
	def openConn(dsrc: => DataSource) : ZIO[Any, Throwable, DbConn] = {
		ZIO.attemptBlocking {
			val dsinfo = describeDataSource(dsrc)
			val conn = dsrc.getConnection
			conn.setAutoCommit(false)
			val dbc = new RoachDbConnImpl(conn)
			(dbc, dsinfo)
		}.debug(".openConn").tap(pair => {
			ZIO.log(s"Opened DataSource with info: ${pair._2}")
		}).map(_._1)
	}
	def closeConn(dbConn : DbConn) : ZIO[Any, Throwable, Unit] = {
		ZIO.attemptBlocking {
			dbConn.getSqlConn.close()
		}.debug(".closeConn")
	}
	def closeAndLogErrors(dbConn : DbConn) : ZIO[Any, Nothing, Unit] = {
		val closeEff = closeConn(dbConn)
		closeEff.orElse(ZIO.logError("Problem closing dbConn"))
	}
	def scopedConn(dsrc: => DataSource): ZIO[Scope, Throwable, DbConn] = {
		ZIO.acquireRelease (openConn(dsrc)) (closeAndLogErrors(_))
	}

	def describeDataSource(dsrc : DataSource) : String = {
		dsrc match {
			case pgds : BaseDataSource => s"DataSourceURL: [${pgds.getURL}]"
			case _ => s"Not a Postgres BaseDataSource: ${dsrc}"
		}
	}
}

object RoachDbConnLayers {
	val myConnOps = new RoachDbConnOps {}
	val dbcLayer01: ZLayer[Any, Throwable, DbConn] = ZLayer.scoped(myConnOps.scopedConn(RoachDataSources.makePGDS))
}