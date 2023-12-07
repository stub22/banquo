package com.appstract.banquo.impl.bank

import com.appstract.banquo.api.bank.AccountOpResultTypes.{AccountHistory, AcctOpResult}
import com.appstract.banquo.api.bank.BankScalarTypes.{AccountID, BalanceAmount}
import com.appstract.banquo.api.roach.DbOpResultTypes.DbOpResult
import com.appstract.banquo.api.bank.{AccountDetails, AccountOpProblem, AcctOpError, AcctOpFailedNoAccount, BalanceChangeSummary, BankAccountReadOps}
import com.appstract.banquo.api.roach.{BalanceChangeInternal, DbConn, DbEmptyResult}
import com.appstract.banquo.impl.roach.RoachReader
import zio.{RIO, URIO, ZIO}


class BankAccountReadOpsImpl extends BankAccountReadOps {
	val myRoachReader = new RoachReader {}

	override def fetchAccountInfo(acctID: AccountID): URIO[DbConn, AcctOpResult[(AccountDetails, BalanceAmount)]] = {
		val detSelJob: RIO[DbConn, DbOpResult[AccountDetails]] = myRoachReader.selectAccountDetails(acctID)
		val balChkJob = myRoachReader.selectLastBalanceChange(acctID)
		val x = for {
			acctDetails <- detSelJob
			balRec <- balChkJob
		} yield ()
		ZIO.succeed(Left(AcctOpError("fetchAccountInfo", acctID, "Not implemented yet!")))
	}

	val MAX_CHANGE_RECORDS = 200

	override def fetchAccountHistory(acctID: AccountID): URIO[DbConn, AcctOpResult[AccountHistory]] = {
		val OP_NAME = "fetchAccountHistory"
		val recentChangesJob: URIO[DbConn, DbOpResult[Seq[BalanceChangeInternal]]] =
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
