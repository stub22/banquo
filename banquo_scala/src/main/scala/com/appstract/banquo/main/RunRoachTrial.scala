package com.appstract.banquo.main

import com.appstract.banquo.impl.roach.{RoachDataSources, RoachSchema}
import org.postgresql.ds.PGSimpleDataSource

import java.sql.{Connection => SQL_Conn}
import java.util.UUID

object RunRoachTrial {
	def main(args: Array[String]): Unit = {
		println("RunRoachTrial: BEGIN")
		runTrial
		println("RunRoachTrial: END")
	}

	def runTrial() : Unit = {
		val pgds = RoachDataSources.makePGDS
		val tp = new TrialPrelim {}
		tp.pingDB(pgds)
		// tp.go(pgds)
	}

}


trait TrialPrelim {
	def pingDB(pgds : PGSimpleDataSource): Unit = {
		val info = pgds.getDescription
		val generatedDriverUrl = pgds.getURL
		println(s"Pgds data source info: [${info}]\ndriverURL=${generatedDriverUrl}")
		val conn: SQL_Conn = pgds.getConnection
		println(s"Got conn: ${conn}")
		conn.setAutoCommit(false)
		val uid01 = UUID.randomUUID();
		println(s"Made uid01: ${uid01.toString} ") // with t-stamp: ${uid01.timestamp()}")
		conn.commit()
		conn.close()
	}
	val schema = RoachSchema
	def go(pgds : PGSimpleDataSource) = {
		implicit val conn: SQL_Conn = pgds.getConnection
		conn.setAutoCommit(false)
		// schema.createTablesAsNeeded(conn)
		// conn.commit()
		// val baw = new BankAccountWriteOps{}

		conn.close()
	}
}

