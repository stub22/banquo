package com.appstract.banquo.api.roach

import com.appstract.banquo.api.bank.BalanceChangeSummary
import com.appstract.banquo.api.bank.BankScalarTypes._

/**
 * Internal query result types, not exposed to HTTP svc.
 */
case class BalanceChangeInternal(changeId : BalanceChangeID, acctID : AccountID, changeFlavor : ChangeFlavor,
								 prevChangeID_opt : Option[BalanceChangeID], changeAmt: ChangeAmount,
								 balanceAmt : BalanceAmount, createTimestamp: Timestamp) {
	def toSummary = BalanceChangeSummary(acctID, changeAmt, balanceAmt, createTimestamp)
}
