package com.appstract.banquo.api

import zio.{URIO, ZIO}

import com.appstract.banquo.api.AccountOpResultTypes.AcctOpResult
import com.appstract.banquo.api.BankScalarTypes._


trait BankAccountWriteOps {
	def makeAccount(customerName: String, customerAddress: String, initBal: BalanceAmount): URIO[DbConn, AcctOpResult[AccountId]]

	def storeBalanceChange(acctID: AccountId, changeAmt: ChangeAmount): URIO[DbConn, AcctOpResult[Unit]]
}
