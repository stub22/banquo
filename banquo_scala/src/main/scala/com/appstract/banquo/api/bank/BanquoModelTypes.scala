package com.appstract.banquo.api.bank

import com.appstract.banquo.api.bank.BankScalarTypes._

///////////////////////////////////////////////////////////////////////////////////////////////////////
// Our input types received as JSON blobs in HTTP requests:
///////////////////////////////////////////////////////////////////////////////////////////////////////

case class XactInput(account_id: AccountID, amount: ChangeAmount, description: XactDescription)


///////////////////////////////////////////////////////////////////////////////////////////////////////
// Our output type(s) used to build HTTP responses
///////////////////////////////////////////////////////////////////////////////////////////////////////

case class AccountSummary(accountID : AccountID, customerName: CustomerName, customerAddress: CustomerAddress, balanceAmt: BalanceAmount)

// DbTimestamp is not trivially JSON-encodable via DeriveEncoder, so we made createTimestampTxt a plain String.
case class BalanceChangeSummary(acctID: AccountID, changeAmt: ChangeAmount, balanceAmt: BalanceAmount,
								createTimestampTxt : String, xactDescription_opt: Option[XactDescription])

///////////////////////////////////////////////////////////////////////////////////////////////////////
// High level errors/failures, to be translated into HTTP error responses
///////////////////////////////////////////////////////////////////////////////////////////////////////

trait AccountOpProblem
case class AcctOpFailedNoAccount(opName : String, accountID: AccountID, details : String) extends AccountOpProblem
case class AcctOpFailedInsufficientFunds(opName : String, accountID: AccountID, details : String) extends AccountOpProblem
case class AcctCreateFailed(details : String) extends AccountOpProblem
case class AcctOpError(opName : String, accountId: AccountID, details : String) extends AccountOpProblem

object AccountOpResultTypes {
	type AcctOpResult[X] = Either[AccountOpProblem, X]

	// TODO: AccountHistory could eventually be some kind of paged result set, or stream.
	//  But currently it is just a single finite sequence.
	type AccountHistory = Seq[BalanceChangeSummary]
}



