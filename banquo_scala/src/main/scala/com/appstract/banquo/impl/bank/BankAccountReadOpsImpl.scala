package com.appstract.banquo.impl.bank

import com.appstract.banquo.api.AccountOpResultTypes.{AccountHistory, AcctOpResult}
import com.appstract.banquo.api.BankScalarTypes.{AccountId, BalanceAmount}
import com.appstract.banquo.api.{AccountDetails, AcctOpError, BankAccountReadOps, DbConn}
import com.appstract.banquo.impl.roach.RoachReader
import zio.{RIO, URIO, ZIO}


class BankAccountReadOpsImpl extends BankAccountReadOps {
	val myRoachReader = new RoachReader {}

	override def fetchAccountInfo(acctID: AccountId): URIO[DbConn, AcctOpResult[(AccountDetails, BalanceAmount)]] = {
		val detSelJob: RIO[DbConn, AccountDetails] = myRoachReader.selectAccountDetails(acctID)
		val balChkJob = myRoachReader.selectLastBalanceChange(acctID)
		ZIO.succeed(Left(AcctOpError("fetchAccountInfo", acctID, "Not implemented yet!")))
	}

	override def fetchAccountHistory(acctID: AccountId): URIO[DbConn, AcctOpResult[AccountHistory]] = {
		ZIO.succeed(Left(AcctOpError("fetchAccountHistory", acctID, "Not implemented yet!")))
	}
}
