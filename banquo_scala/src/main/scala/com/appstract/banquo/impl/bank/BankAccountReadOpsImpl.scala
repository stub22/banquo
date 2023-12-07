package com.appstract.banquo.impl.bank

import com.appstract.banquo.api.bank.AccountOpResultTypes.{AccountHistory, AcctOpResult}
import com.appstract.banquo.api.bank.BankScalarTypes.{AccountID, BalanceAmount}
import com.appstract.banquo.api.roach.DbOpResultTypes.DbOpResult
import com.appstract.banquo.api.bank.{AccountOpProblem, AccountSummary, AcctOpError, AcctOpFailedNoAccount, BalanceChangeSummary, BankAccountReadOps}
import com.appstract.banquo.api.roach.{AccountDetails, BalanceChangeDetails, DbConn, DbEmptyResult}
import com.appstract.banquo.impl.roach.RoachReader
import zio.{RIO, URIO, ZIO}


class BankAccountReadOpsImpl extends BankAccountReadOps {
	val myRoachReader = new RoachReader {}

	override def fetchAccountInfo(acctID: AccountID): URIO[DbConn, AcctOpResult[AccountSummary]] = {
		val OP_NAME = "fetchAccountInfo"
		val detailsJob: URIO[DbConn, DbOpResult[AccountDetails]] = myRoachReader.selectAccountDetails(acctID)
		val summaryJob: ZIO[DbConn, Nothing, AcctOpResult[AccountSummary]] = detailsJob.flatMap(_  match {
			case Left(acctEmpty : DbEmptyResult) => ZIO.succeed(Left(AcctOpFailedNoAccount(OP_NAME, acctID, acctEmpty.toString)))
			case Left(otherErr) => ZIO.succeed(Left(AcctOpError(OP_NAME, acctID, otherErr.toString)))
			case Right(acctDetails) => myRoachReader.selectLastBalanceChange(acctID).map(_ match {
					case Left(balErr) => Left(AcctOpError(OP_NAME, acctID, balErr.toString))
					case Right(balChgRec) => Right(buildAccountSummary(acctDetails, balChgRec))
			})
		})
		summaryJob
	}

	private def buildAccountSummary(acctDetails : AccountDetails, balChg : BalanceChangeDetails) : AccountSummary = {
		AccountSummary(acctDetails.accountID, acctDetails.customerName, acctDetails.customerAddress, balChg.balanceAmt)
	}

	val MAX_CHANGE_RECORDS = 200

	override def fetchAccountHistory(acctID: AccountID): URIO[DbConn, AcctOpResult[AccountHistory]] = {
		val OP_NAME = "fetchAccountHistory"
		val recentChangesJob: URIO[DbConn, DbOpResult[Seq[BalanceChangeDetails]]] =
				myRoachReader.selectRecentBalanceChanges(acctID, MAX_CHANGE_RECORDS)
		val fetchHistoryJob: URIO[DbConn, AcctOpResult[Seq[BalanceChangeSummary]]] =
			recentChangesJob.map(dbOpRslt => dbOpRslt.fold(
				dbErr => dbErr match {
					case empty: DbEmptyResult => Left(AcctOpFailedNoAccount(OP_NAME, acctID, empty.toString))
					case other => Left(AcctOpError(OP_NAME, acctID, other.toString))
				},
				history => Right(history.map(bcInternal => bcInternal.toSummary))
			))
		fetchHistoryJob
	}
}
