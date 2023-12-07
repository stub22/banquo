package com.appstract.banquo.api

import zio.URIO
import com.appstract.banquo.api.AccountOpResultTypes.{AccountHistory, AcctOpResult}
import com.appstract.banquo.api.BankScalarTypes.{AccountID, BalanceAmount}


trait BankAccountReadOps {
	def fetchAccountInfo(acctId: AccountID): URIO[DbConn, AcctOpResult[(AccountDetails, BalanceAmount)]]

	// TODO: AccountHistory hould be some kind of paged result set, or stream.
	def fetchAccountHistory(acctId: AccountID): URIO[DbConn, AcctOpResult[AccountHistory]]
}
