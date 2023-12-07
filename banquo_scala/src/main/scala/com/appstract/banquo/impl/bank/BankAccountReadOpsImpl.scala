package com.appstract.banquo.impl.bank

import com.appstract.banquo.api.bank.AccountOpResultTypes.{AccountHistory, AcctOpResult}
import com.appstract.banquo.api.bank.BankScalarTypes.{AccountID, BalanceAmount}
import com.appstract.banquo.api.roach.DbOpResultTypes.DbOpResult
import com.appstract.banquo.api.bank.{AccountDetails, AccountOpProblem, AccountSummary, AcctOpError, AcctOpFailedNoAccount, BalanceChangeSummary, BankAccountReadOps}
import com.appstract.banquo.api.roach.{BalanceChangeInternal, DbConn, DbEmptyResult}
import com.appstract.banquo.impl.roach.RoachReader
import zio.{RIO, URIO, ZIO}


class BankAccountReadOpsImpl extends BankAccountReadOps {
	val myRoachReader = new RoachReader {}

	override def fetchAccountInfo(acctID: AccountID): URIO[DbConn, AcctOpResult[AccountSummary]] = {
		val OP_NAME = "fetchAccountInfo"
		// This code would be shorter if we admitted Cats-core EitherT.
		val detSelJob: URIO[DbConn, DbOpResult[AccountDetails]] = myRoachReader.selectAccountDetails(acctID)
		val balChkJob: URIO[DbConn, DbOpResult[BalanceChangeInternal]] = myRoachReader.selectLastBalanceChange(acctID)
		val comboJob: URIO[DbConn, (DbOpResult[AccountDetails], DbOpResult[BalanceChangeInternal])] = for {
			acctDetailsResultEither <- detSelJob
			balRecResultEither <- balChkJob
		} yield (acctDetailsResultEither, balRecResultEither)
		val summaryJob = comboJob.map(pairOfRsltEithers => {
			pairOfRsltEithers match {
				case(Right(acctDetails), Right(balChange)) => Right(buildAccountSummary(acctDetails, balChange))
				case(Left(acctEmpty : DbEmptyResult), _) => Left(AcctOpFailedNoAccount(OP_NAME, acctID, acctEmpty.toString))
				case(Left(otherErr), _) => Left(AcctOpError(OP_NAME, acctID, otherErr.toString))
				case(_, Left(balErr)) => Left(AcctOpError(OP_NAME, acctID, balErr.toString))
			}
		})
		summaryJob
	}

	private def buildAccountSummary(acctDetails : AccountDetails, balChg : BalanceChangeInternal) : AccountSummary = {
		AccountSummary(acctDetails.accountID, acctDetails.customerName, acctDetails.customerAddress, balChg.balanceAmt)
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
