package com.appstract.banquo.impl.bank

import com.appstract.banquo.api.{BankAccountWriteOps, DbConn}
import zio.ZIO
import com.appstract.banquo.api.BankScalarTypes.{AccountId, BalanceAmount, BalanceChangeId, ChangeAmount}
import com.appstract.banquo.impl.roach.{DbError, RoachReader, RoachWriter}


// Xact stands for "transaction" in the context of a bank account (not a database).
class BankAccountWriteOpsImpl extends BankAccountWriteOps {
	val myRoachWriter = new RoachWriter {}
	val myRoachReader = new RoachReader {}

	/*
	Does NOT auto-commit!
	 */
	override def makeAccount(customerName: String, customerAddress: String, initBal: BalanceAmount):
					ZIO[DbConn, Throwable, (AccountId, BalanceChangeId)] = {
		// Must insert the Account record AND create an initial balance record.
		for {
			acctId <- myRoachWriter.insertAccount(customerName, customerAddress)
			initChgId <- myRoachWriter.insertInitialBalance(acctId, initBal)
		} yield (acctId, initChgId)
	}

	def storeBalanceChange(acctID: AccountId, changeAmt: ChangeAmount): ZIO[Any, DbError, BalanceChangeId] = {
		/*	val combinedResultEith: Either[DbError, BalanceChangeId] = for {
				previousChange <- myRoachReader.selectLastBalanceChange(acctID)
				nxtBalAmt = previousChange.balanceAmt.+(changeAmt)
				nextChgId <- myRoachWriter.insertBalanceChange(acctID, previousChange.changeId, changeAmt, nxtBalAmt)
			} yield(nextChgId)
			ZIO.fromEither(combinedResultEith)
		*/
		???
	}
}
