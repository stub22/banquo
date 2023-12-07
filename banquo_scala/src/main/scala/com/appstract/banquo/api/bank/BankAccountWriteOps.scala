package com.appstract.banquo.api.bank

import com.appstract.banquo.api.bank.AccountOpResultTypes.AcctOpResult
import com.appstract.banquo.api.bank.BankScalarTypes._
import com.appstract.banquo.api.roach.DbConn
import zio.URIO

/** *
 * Each operation produces a ZIO effect that requires wiring to a DbConn service.
 * To make this API more abstract, we could further generalize the type of DbConn to support non-SQL mechanisms.
 *
 * Other than the DbConn wiring, each operation at this level should be self contained, and responsible for
 * performing any required DB commits, error handling and retry behavior.
 */

trait BankAccountWriteOps {

	def makeAccount(customerName: String, customerAddress: String, initBal: BalanceAmount): URIO[DbConn, AcctOpResult[AccountID]]

	def storeBalanceChange(acctID: AccountID, changeAmt: ChangeAmount, xactDesc : XactDescription): URIO[DbConn, AcctOpResult[BalanceChangeSummary]]

}
