package com.appstract.banquo.main

import com.appstract.banquo.api.bank.AccountOpResultTypes.AcctOpResult
import com.appstract.banquo.api.bank.BankScalarTypes.AccountID
import com.appstract.banquo.api.roach.DbConn
import com.appstract.banquo.impl.bank.BankAccountWriteOpsImpl
import com.appstract.banquo.impl.roach.{RoachDbConnLayers, RoachSchema, SqlEffectMaker}
import zio.stream.ZStream
import zio.{Scope, Task, URIO, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

import scala.math.BigDecimal.RoundingMode
import scala.util.Random

/***
 * This standalone program so far does 2 things, then exits.
 * 	1) Ensure that our SQL schema has been created.
 * 	2) Insert NUM_DUMMY_ACCOUNTS into the DB.
 */

object RunRoachTests extends ZIOAppDefault {
	val mySqlExec = new SqlEffectMaker

	val NUM_DUMMY_ACCOUNTS = 5

	override def run: Task[Any] = {

		val testDbLayer: ZLayer[Any, Throwable, DbConn] = RoachDbConnLayers.dbcLayer01

		val tableCreateJob = setupSchema.debug("Banquo SQL schema setup is done, including DB commit")

		val dummyAccountsJob: ZIO[DbConn, Nothing, Vector[AcctOpResult[AccountID]]] = mkDummyAccounts(NUM_DUMMY_ACCOUNTS)

		val comboOp = tableCreateJob *> dummyAccountsJob

		val appToRun = comboOp.provideLayer(testDbLayer)

		appToRun.debug(".appToRun")
	}

	def setupSchema: ZIO[DbConn, Throwable, Unit] = {
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





