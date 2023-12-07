package com.appstract.banquo.main

import com.appstract.banquo.api.AccountOpResultTypes.AcctOpResult
import com.appstract.banquo.api.BankScalarTypes.AccountID
import com.appstract.banquo.api.DbConn
import com.appstract.banquo.impl.bank.BankAccountWriteOpsImpl
import com.appstract.banquo.impl.roach.{RoachDbConnLayers, RoachSchema, SqlEffectMaker}
import zio.stream.{ZStream}
import zio.{Scope, URIO, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

import scala.math.BigDecimal.RoundingMode
import scala.util.Random

object RunRoachTests extends ZIOAppDefault {
	val mySqlExec = new SqlEffectMaker

	override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {

		val testDbLayer: ZLayer[Any, Throwable, DbConn] = RoachDbConnLayers.dbcLayer01

		val tableCreateJob = setupSchema.debug("schema setup is done, and committed")

		val dummyAccountsJob: ZIO[DbConn, Nothing, Vector[AcctOpResult[AccountID]]] = mkDummyAccounts(5)

		val comboOp = tableCreateJob *> dummyAccountsJob

		val appToRun = comboOp.provideLayer(testDbLayer)
		appToRun.debug(".appToRun")
	}

	def setupSchema = {
		val schemaCreateJob = RoachSchema.createTablesAsNeeded
		// We need an explicit commit for CockroachDB to absorb DDL statements.
		val commitJob = mySqlExec.execCommit.debug(".execCommit (after .createTablesAsNeeded)")
		schemaCreateJob *> commitJob
	}
	/***
	 * Creates N accounts with random initial balances.
	 * Uses a single JDBC connection, committing after each account is created.
	 * Outputs a sequence of the created account IDs.
	 */
	def mkDummyAccounts(numAccts : Int): ZIO[DbConn, Nothing, Vector[AcctOpResult[AccountID]]] = {
		val dummyAccountJobNeedsDbc = mkDummyAccountOp
		// This would create the accounts, but it does not collect the results for us.
		// val repeatedJobUsesOneDbc: ZIO[DbConn, Nothing, AcctOpResult[AccountID]] = dummyAccountJobNeedsDbc.repeatN(numAccts)
		val strmOne: ZStream[DbConn, Nothing, AcctOpResult[AccountID]] = ZStream.fromZIO(dummyAccountJobNeedsDbc)
		val strmMany: ZStream[DbConn, Nothing, AcctOpResult[AccountID]] = ZStream.repeatZIO(dummyAccountJobNeedsDbc)
		val strmN: ZStream[DbConn, Nothing, AcctOpResult[AccountID]] = strmMany.take(numAccts)
		val outN = strmN.runFold(Vector[AcctOpResult[AccountID]]())((prevVec, nxtRslt) => prevVec :+ nxtRslt)
		outN.debug(s"mkDummyAccounts(${numAccts})")
	}
	def mkDummyAccountOp : URIO[DbConn, AcctOpResult[AccountID]] = {
		val genJob = ZIO.succeedBlocking {
			val ts: Long = System.currentTimeMillis()
			val dummyName = "dummy_" + ts
			val DUMMY_AMOUNT_MAX = 1000.0f
			val rndFloat = Random.nextFloat() * DUMMY_AMOUNT_MAX
			val rndAmtBD = BigDecimal(rndFloat).setScale(2, RoundingMode.HALF_DOWN)
			(dummyName, rndAmtBD)
		}
		val acctWriteOps = new BankAccountWriteOpsImpl {}
		val makeDummyAccountOp: URIO[DbConn, AcctOpResult[AccountID]] = genJob.flatMap(dataPair => {
			val (name, amt) = dataPair
			acctWriteOps.makeAccount(name, name, amt)
		})
		makeDummyAccountOp.debug(".makeDummyAccountOp")
	}

}





