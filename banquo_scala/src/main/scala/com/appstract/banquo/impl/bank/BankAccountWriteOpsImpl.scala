package com.appstract.banquo.impl.bank

import zio.{URIO, ZIO}
import com.appstract.banquo.api.AccountOpResultTypes.AcctOpResult
import com.appstract.banquo.api.{AcctCreateFailed, AcctOpError, BalanceChangeSummary, BankAccountWriteOps, DbConn, DbProblem}
import com.appstract.banquo.api.BankScalarTypes.{AccountId, BalanceAmount, BalanceChangeId, ChangeAmount}
import com.appstract.banquo.impl.roach.{RoachReader, RoachWriter, SqlExecutor}


/**
 * These high-level write operations are responsible for their own database commits, error handling, and retry behavior.
 **/

class BankAccountWriteOpsImpl extends BankAccountWriteOps {
	val myRoachWriter = new RoachWriter {}
	val myRoachReader = new RoachReader {}
	private val mySqlExec = new SqlExecutor
	private val commitJob: ZIO[DbConn, Throwable, Unit] = mySqlExec.execCommit()


	override def makeAccount(customerName: String, customerAddress: String, initBal: BalanceAmount):
				URIO[DbConn, AcctOpResult[AccountId]] = {
		// Must insert the Account record AND create an initial balance record.
		val op: ZIO[DbConn, Throwable, (AccountId, BalanceChangeId)] = for {
			acctId <- myRoachWriter.insertAccount(customerName, customerAddress)
			initChgId <- myRoachWriter.insertInitialBalance(acctId, initBal)
		} yield (acctId, initChgId)
		val opWithCommit = op <* commitJob
		opWithCommit.catchAll(t => ZIO.succeed(Left(AcctCreateFailed(t.toString)))).map(rsltPair => {
			val (a : AccountId, c : BalanceChangeId) = rsltPair
			Right(a)
		}).debug(".makeAccount result: ")
	}

	override def storeBalanceChange(acctID: AccountId, changeAmt: ChangeAmount): URIO[DbConn, AcctOpResult[BalanceChangeSummary]] = {

		ZIO.succeed(Left(AcctOpError("storeBalanceChange", acctID, "Not implemented yet!")))
	}

}
//	def storeBalanceChange(acctID: AccountId, changeAmt: ChangeAmount): ZIO[Any, DbProblem, BalanceChangeId] = {
		/*	val combinedResultEith: Either[DbError, BalanceChangeId] = for {
				previousChange <- myRoachReader.selectLastBalanceChange(acctID)
				nxtBalAmt = previousChange.balanceAmt.+(changeAmt)
				nextChgId <- myRoachWriter.insertBalanceChange(acctID, previousChange.changeId, changeAmt, nxtBalAmt)
			} yield(nextChgId)
			ZIO.fromEither(combinedResultEith)
		*/

