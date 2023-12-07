package com.appstract.banquo.impl.bank

import zio.{URIO, ZIO}
import com.appstract.banquo.api.AccountOpResultTypes.AcctOpResult
import com.appstract.banquo.api.{AcctCreateFailed, AcctOpError, BankAccountWriteOps, DbConn, DbProblem}
import com.appstract.banquo.api.BankScalarTypes.{AccountId, BalanceAmount, BalanceChangeId, ChangeAmount}
import com.appstract.banquo.impl.roach.{RoachReader, RoachWriter}


// Xact stands for "transaction" in the context of a bank account (not a database).
class BankAccountWriteOpsImpl extends BankAccountWriteOps {
	val myRoachWriter = new RoachWriter {}
	val myRoachReader = new RoachReader {}

	/*
	Does NOT auto-commit!
	 */
	override def makeAccount(customerName: String, customerAddress: String, initBal: BalanceAmount):
			URIO[DbConn, AcctOpResult[AccountId]] = {
		// Must insert the Account record AND create an initial balance record.
		val op: ZIO[DbConn, Throwable, (AccountId, BalanceChangeId)] = for {
			acctId <- myRoachWriter.insertAccount(customerName, customerAddress)
			initChgId <- myRoachWriter.insertInitialBalance(acctId, initBal)
		} yield (acctId, initChgId)
		op.catchAll(t => ZIO.succeed(Left(AcctCreateFailed(t.toString)))).map(rsltPair => {
			val (a : AccountId, c : BalanceChangeId) = rsltPair
			Right(a)
		})
	}

	override def storeBalanceChange(acctID: AccountId, changeAmt: ChangeAmount): URIO[DbConn, AcctOpResult[Unit]] = {
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

