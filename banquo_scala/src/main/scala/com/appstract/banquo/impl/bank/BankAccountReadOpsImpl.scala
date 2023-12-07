package com.appstract.banquo.impl.bank

import com.appstract.banquo.api.AccountOpResultTypes.{AccountHistory, AcctOpResult}
import com.appstract.banquo.api.BankScalarTypes.{AccountId, BalanceAmount}
import com.appstract.banquo.api.{AccountDetails, BankAccountReadOps, DbConn, AcctOpError}
import com.appstract.banquo.impl.roach.RoachReader
import zio.{URIO, ZIO}


class BankAccountReadOpsImpl extends BankAccountReadOps {
	val myRoachReader = new RoachReader {}

	override def fetchAccountInfo(acctId: AccountId): URIO[DbConn, AcctOpResult[(AccountDetails, BalanceAmount)]] = {
		ZIO.succeed(Left(AcctOpError("fetchAccountInfo", acctId, "Not implemented yet!")))
	}

	override def fetchAccountHistory(acctId: AccountId): URIO[DbConn, AcctOpResult[AccountHistory]] = {
		ZIO.succeed(Left(AcctOpError("fetchAccountHistory", acctId, "Not implemented yet!")))
	}
}
