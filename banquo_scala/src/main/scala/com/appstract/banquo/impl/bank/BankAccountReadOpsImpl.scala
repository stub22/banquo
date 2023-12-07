package com.appstract.banquo.impl.bank

import com.appstract.banquo.api.AccountOpResultTypes.{AccountHistory, AcctOpResult}
import com.appstract.banquo.api.BankScalarTypes.{AccountID, BalanceAmount}
import com.appstract.banquo.api.DbOpResultTypes.DbOpResult
import com.appstract.banquo.api.{AccountDetails, AcctOpError, BankAccountReadOps, DbConn}
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
		val recentChangesJob = myRoachReader.selectRecentBalanceChanges(acctID, MAX_CHANGE_RECORDS)
		ZIO.succeed(Left(AcctOpError("fetchAccountHistory", acctID, "Not implemented yet!")))
	}
}
