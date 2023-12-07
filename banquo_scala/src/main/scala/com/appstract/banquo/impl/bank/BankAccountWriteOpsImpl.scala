package com.appstract.banquo.impl.bank

import zio.{ URIO, ZIO}
import com.appstract.banquo.api.bank.AccountOpResultTypes.AcctOpResult
import com.appstract.banquo.api.bank.BankScalarTypes.{AccountID, BalanceAmount, BalanceChangeID, ChangeAmount, XactDescription}
import com.appstract.banquo.api.roach.DbOpResultTypes.DbOpResult
import com.appstract.banquo.api.bank.{AcctCreateFailed, AcctOpError, AcctOpFailedInsufficientFunds, AcctOpFailedNoAccount, BalanceChangeSummary, BankAccountWriteOps}
import com.appstract.banquo.api.roach.{BalanceChangeDetails, DbConn, DbEmptyResult}
import com.appstract.banquo.impl.roach.{RoachReader, RoachWriter, SqlEffectMaker}

/**
 * These high-level write operations are responsible for their own database commits, error handling, and retry behavior.
 **/

class BankAccountWriteOpsImpl extends BankAccountWriteOps {
	val myRoachWriter = new RoachWriter {}
	val myRoachReader = new RoachReader {}

	private val mySqlJobMaker = new SqlEffectMaker
	private val myCommitJob: ZIO[DbConn, Throwable, Unit] = mySqlJobMaker.execCommit()

	/** This method implements a feature that is not accessed by our web service, and not directly required by our
	 * specification.  However we obviously need to make accounts in order to test our required features, and we would
	 * like accounts to be created consistently.  (Two INSERTS are required).
	 * It seems that CockroachDB does not support Stored Procedures (as of 2023-12).
	 *
	 * This kind of sequencing of Effect[Either] results is a little easier if we use cats-core EitherT.
	 * We *could* use that here if we added the dependency, but for now we are sticking with a ZIO-only approach.
	 * We include the full types for many of the intermediate results, which may help some readers while annoying others!
	 */
	override def makeAccount(customerName: String, customerAddress: String, initBal: BalanceAmount):
				URIO[DbConn, AcctOpResult[AccountID]] = {
		// Must insert the Account record AND THEN insert an initial balance record.
		// We must also commit the DB transaction.
		val acctInsertJob: ZIO[DbConn, Throwable, DbOpResult[AccountID]] = myRoachWriter.insertAccount(customerName, customerAddress)
		val combinedInsertJob: ZIO[DbConn, Throwable, DbOpResult[(AccountID, BalanceChangeID)]] = acctInsertJob.flatMap(acctRsltEith => {
			// TODO: Consider using Cats-core EitherT here, to make the code more concise.
			acctRsltEith.fold(
				dbProblem => ZIO.succeed(Left(dbProblem)),
				// TODO:  If balance-insert fails, we should .rollback to void the account insert.
				// But in current impl, we believe that rollback happens implicitly when our DB conn is closed.
				acctID => myRoachWriter.insertInitialBalance(acctID, initBal).map(_.map((acctID, _))))
		}).debug(".makeAccount combinedInsertJob result, before commit")

		val opWithCommit: ZIO[DbConn, Throwable, DbOpResult[(AccountID, BalanceChangeID)]] =
				combinedInsertJob <* myCommitJob

		// Again here our code could be a bit briefer if we used Cats EitherT.
		val opWithSimpleResult = opWithCommit.map(rsltPairEither =>
			rsltPairEither.fold(
				dbError => Left(AcctCreateFailed(dbError.toString)), 	// Error case
				resultPair => Right(resultPair._1)))					// Success case

		// Attach a handler for any not-yet-mapped exceptions generated during commit.
		val opWithErrHandling: URIO[DbConn, AcctOpResult[AccountID]] =
			opWithSimpleResult.catchAll(t => ZIO.succeed(Left(AcctCreateFailed(t.toString))))
		opWithErrHandling.debug(".makeAccount final result")
	}

	override def storeBalanceChange(acctID: AccountID, changeAmt: ChangeAmount, xactDesc : XactDescription):
									URIO[DbConn, AcctOpResult[BalanceChangeSummary]] = {
		// We must first lookup the PREVIOUS balance change for this account.
		val OP_NAME = "storeBalanceChange"
		val prevBalChgJob: URIO[DbConn, DbOpResult[BalanceChangeDetails]] = myRoachReader.selectLastBalanceChange(acctID)
		val balChgStoreOp = prevBalChgJob.flatMap(_ match {
			case Left(balEmpty: DbEmptyResult) => ZIO.succeed(Left(AcctOpFailedNoAccount(OP_NAME, acctID, balEmpty.toString)))
			case Left(otherErr) => ZIO.succeed(Left(AcctOpError(OP_NAME, acctID, otherErr.toString)))
			case Right(prevBalChg) => {
				val nxtBalAmt = prevBalChg.balanceAmt.+(changeAmt)
				if (nxtBalAmt.sign >= 0) {
					val insertResult: URIO[DbConn, DbOpResult[BalanceChangeSummary]] =
						myRoachWriter.insertBalanceChange(acctID, prevBalChg.changeID, changeAmt, nxtBalAmt, xactDesc)
					insertResult.map(_.fold(
						dbErr => Left(AcctOpError(OP_NAME, acctID, dbErr.toString)),
						chgSumm => Right(chgSumm)
					))
				}
				else ZIO.succeed(Left(AcctOpFailedInsufficientFunds(OP_NAME, acctID,
					s"previousBalance=${prevBalChg}, change=${changeAmt}")))
			}
		})
		val opWithCommit = balChgStoreOp <* myCommitJob
		val opWithErrHandling: URIO[DbConn, AcctOpResult[BalanceChangeSummary]] =
			opWithCommit.catchAll(t => ZIO.succeed(Left(AcctOpError(OP_NAME, acctID, t.toString))))
		opWithErrHandling
	}

}


