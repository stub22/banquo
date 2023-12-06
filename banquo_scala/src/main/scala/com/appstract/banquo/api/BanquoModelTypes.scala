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

trait AccountOpProblem
case class AcctOpFailedNoAccount(opName : String, accountId: AccountId, details : String) extends AccountOpProblem
case class AcctOpFailedInsufficientFunds(opName : String, accountId: AccountId, details : String) extends AccountOpProblem
case class AcctCreateFailed(details : String) extends AccountOpProblem
case class AcctOpError(opName : String, accountId: AccountId, details : String) extends AccountOpProblem

object AccountOpResultTypes {
	type AcctOpResult[X] = Either[AccountOpProblem, X]

	// TODO: AccountHistory hould be some kind of paged result set, or stream
	type AccountHistory = Seq[BalanceChange]
}