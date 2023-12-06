package com.appstract.banquo.impl.bank

import com.appstract.banquo.api.AccountOpResultTypes.{AccountHistory, AcctOpResult}
import com.appstract.banquo.api.BankScalarTypes.{AccountId, BalanceAmount}
import com.appstract.banquo.api.{AccountDetails, BankAccountReadOps, DbConn}
import com.appstract.banquo.impl.roach.RoachReader
import zio.URIO


class BankAccountReadOpsImpl extends BankAccountReadOps {
	val myRoachReader = new RoachReader {}

	override def fetchAccountInfo(acctId: AccountId): URIO[DbConn, AcctOpResult[(AccountDetails, BalanceAmount)]] = ???

	override def fetchAccountHistory(acctId: AccountId): URIO[DbConn, AcctOpResult[AccountHistory]] = ???
}
