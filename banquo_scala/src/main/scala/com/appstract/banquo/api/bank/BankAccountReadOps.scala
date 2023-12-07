package com.appstract.banquo.api.bank

import com.appstract.banquo.api.bank.AccountOpResultTypes.{AccountHistory, AcctOpResult}
import com.appstract.banquo.api.bank.BankScalarTypes.{AccountID, BalanceAmount}
import com.appstract.banquo.api.roach.DbConn
import zio.URIO


trait BankAccountReadOps {
	def fetchAccountInfo(acctId: AccountID): URIO[DbConn, AcctOpResult[(AccountDetails, BalanceAmount)]]

	// TODO: AccountHistory could be some kind of paged result set, or stream.
	def fetchAccountHistory(acctId: AccountID): URIO[DbConn, AcctOpResult[AccountHistory]]
}
