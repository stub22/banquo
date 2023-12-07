package com.appstract.banquo.api.bank

import com.appstract.banquo.api.bank.BankScalarTypes._


/**
 * Our result types reported to the HTTP service.
 */
case class AccountDetails(accountID : AccountID, customerName: CustomerName, customerAddress: CustomerAddress,
						  createTimestamp: Timestamp)

case class BalanceChangeSummary(acctID: AccountID, changeAmt: ChangeAmount, balanceAmt: BalanceAmount,
						createTimestamp: Timestamp)

trait AccountOpProblem
case class AcctOpFailedNoAccount(opName : String, accountID: AccountID, details : String) extends AccountOpProblem
case class AcctOpFailedInsufficientFunds(opName : String, accountID: AccountID, details : String) extends AccountOpProblem
case class AcctCreateFailed(details : String) extends AccountOpProblem
case class AcctOpError(opName : String, accountId: AccountID, details : String) extends AccountOpProblem

object AccountOpResultTypes {
	type AcctOpResult[X] = Either[AccountOpProblem, X]

	// TODO: AccountHistory could be some kind of paged result set, or stream
	// Currently it is just a single finite sequence.
	type AccountHistory = Seq[BalanceChangeSummary]
}



