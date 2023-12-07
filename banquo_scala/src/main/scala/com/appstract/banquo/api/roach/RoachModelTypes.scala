package com.appstract.banquo.api.roach

import com.appstract.banquo.api.bank.BalanceChangeSummary
import com.appstract.banquo.api.bank.BankScalarTypes._

/**
 * Internal query result types, not exposed to HTTP svc.
 */

case class AccountDetails(accountID: AccountID, customerName: CustomerName, customerAddress: CustomerAddress,
						  createTimestamp: DbTimestamp)

case class BalanceChangeDetails(changeID : BalanceChangeID, acctID : AccountID, changeFlavor : ChangeFlavor,
				prevChangeID_opt : Option[BalanceChangeID], changeAmt: ChangeAmount,
				balanceAmt : BalanceAmount, createTimestamp: DbTimestamp, xactDescription_opt: Option[XactDescription]) {

	def toSummary = BalanceChangeSummary(acctID, changeAmt, balanceAmt, createTimestamp.toString, xactDescription_opt)
}


