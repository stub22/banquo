package com.appstract.banquo.api.bank

import com.appstract.banquo.api.bank.BankScalarTypes._



case class XactInput(account_id: AccountID, amount: ChangeAmount, description: XactDescription)
/**
 * Our result types reported to the HTTP service.
 */

case class AccountSummary(accountID : AccountID, customerName: CustomerName, customerAddress: CustomerAddress, balanceAmt: BalanceAmount)


case class BalanceChangeSummary(acctID: AccountID, changeAmt: ChangeAmount, balanceAmt: BalanceAmount,
						createTimestampTxt : String) // : DbTimestamp is not trivially JSON-encodable for DeriveEncoder.

trait AccountOpProblem
case class AcctOpFailedNoAccount(opName : String, accountID: AccountID, details : String) extends AccountOpProblem
case class AcctOpFailedInsufficientFunds(opName : String, accountID: AccountID, details : String) extends AccountOpProblem
case class AcctCreateFailed(details : String) extends AccountOpProblem
case class AcctOpError(opName : String, accountId: AccountID, details : String) extends AccountOpProblem

object AccountOpResultTypes {
	type AcctOpResult[X] = Either[AccountOpProblem, X]

	// TODO: AccountHistory could be some kind of paged result set, or stream.  But currently it is just a single finite sequence.
	type AccountHistory = Seq[BalanceChangeSummary]
}


