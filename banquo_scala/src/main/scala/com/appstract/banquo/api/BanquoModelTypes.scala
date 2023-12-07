package com.appstract.banquo.api

import java.sql.{Timestamp => JsqlTimestamp}

private trait BanquoModelTypes

object BankScalarTypes {
	type AccountID = String
	type CustomerName = String
	type CustomerAddress = String
	type Timestamp = JsqlTimestamp

	type BalanceChangeID = Long
	type ChangeAmount = BigDecimal
	type BalanceAmount = BigDecimal
	type ChangeFlavor = String // TODO:  This is an SQL enum, which we might map into our Scala types in various ways.
}

import com.appstract.banquo.api.BankScalarTypes._

case class AccountDetails(accountID : AccountID, customerName: CustomerName, customerAddress: CustomerAddress, createTimestamp: Timestamp)

case class BalanceChange(changeId : BalanceChangeID, acctID : AccountID, changeFlavor : ChangeFlavor,
						 prevChangeID_opt : Option[BalanceChangeID], changeAmt: ChangeAmount, balanceAmt : BalanceAmount,
						 createTimestamp: Timestamp)

case class BalanceChangeSummary(acctID: AccountID, changeAmt: ChangeAmount, balanceAmt: BalanceAmount, createTimestamp: Timestamp)

trait AccountOpProblem
case class AcctOpFailedNoAccount(opName : String, accountID: AccountID, details : String) extends AccountOpProblem
case class AcctOpFailedInsufficientFunds(opName : String, accountID: AccountID, details : String) extends AccountOpProblem
case class AcctCreateFailed(details : String) extends AccountOpProblem
case class AcctOpError(opName : String, accountId: AccountID, details : String) extends AccountOpProblem

object AccountOpResultTypes {
	type AcctOpResult[X] = Either[AccountOpProblem, X]

	// TODO: AccountHistory hould be some kind of paged result set, or stream
	type AccountHistory = Seq[BalanceChangeSummary]
}