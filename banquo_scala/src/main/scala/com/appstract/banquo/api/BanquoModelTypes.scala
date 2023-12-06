package com.appstract.banquo.api

private trait BanquoModelTypes

object BankScalarTypes {
	type AccountId = String
	type CustomerName = String
	type CustomerAddress = String
	type BalanceChangeId = Long
	type ChangeAmount = BigDecimal
	type BalanceAmount = BigDecimal
}

import com.appstract.banquo.api.BankScalarTypes._

case class AccountDetails(acctID : AccountId, customerName: CustomerName, customerAddress: CustomerAddress)

case class BalanceChange(changeId : BalanceChangeId, acctID : AccountId, prevChangeId_opt : Option[BalanceChangeId],
						 changeAmt: ChangeAmount, balanceAmt : BalanceAmount)
