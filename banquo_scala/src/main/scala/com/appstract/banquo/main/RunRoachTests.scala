package com.appstract.banquo.main

import com.appstract.banquo.api.DbConn
import com.appstract.banquo.impl.bank.BankAccountWriteOpsImpl
import com.appstract.banquo.impl.roach.{RoachDbConnLayers, RoachSchema, RoachWriter, SqlEffectMaker}
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

object RunRoachTests extends ZIOAppDefault {

	override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {

		val dummyDbOp = ZIO.serviceWithZIO[DbConn](dbc => {
			doFakeOpWithConn(dbc)
		}).debug("zioThatNeedsDbConn debug")

		val tableCreateOp = setup.debug("schema setup")

		val rw = new RoachWriter {}
		val ts = System.currentTimeMillis()
		val dummyName = "dummy_" + ts
		val dummyInsertOp = rw.insertAccount(dummyName, dummyName)

		val bawo = new BankAccountWriteOpsImpl {}
		val makeDummyAccountOp = bawo.makeAccount(dummyName, dummyName, BigDecimal("72.50")).debug(".run makeDummyAccountOp")

		// makeAccount now does its own commit.
		// val commitOp = mySqlExec.execCommit().debug(".run commit after makeDummyAccount")
		// val z1 = baw.makeAccount("Milton Friedman", "123 Main St, Anytown USA", BigDecimal("100.0"))
		//	val z2 = baw.makeAccount("John Keynes", "456 Andover St, Liverpool UK",  BigDecimal("200.0"))
		val comboOp = tableCreateOp *> makeDummyAccountOp // *> commitOp//  dummyDbOp *> tableCreateOp

		val appToRun = comboOp.provideLayer(RoachDbConnLayers.dbcLayer01)
		appToRun.debug(".appToRun")
	}


	val mySqlExec = new SqlEffectMaker
	val schema = RoachSchema
	def setup = {
		val schemaCreateJob = schema.createTablesAsNeeded
		// Empirically, it seems this commit is required for CockroachDB to absorb DDL statements.
		val commitJob = mySqlExec.execCommit.debug(".execCommit (after .createTablesAsNeeded)")
		schemaCreateJob *> commitJob
	}

	private def doFakeOpWithConn(dbc: DbConn) = {
		val sqlConn = dbc.getSqlConn
		mySqlExec.execUpdateNoResult("DROP TABLE dummy_dumdum")
	}

}





