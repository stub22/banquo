package com.appstract.banquo.api

import zio.{URIO, ZIO}

import com.appstract.banquo.api.AccountOpResultTypes.AcctOpResult
import com.appstract.banquo.api.BankScalarTypes._

/** *
 * Each operation produces a ZIO effect that requires wiring to a DbConn service.
 * To make this API more abstract, we could further generalize the type of DbConn to support non-SQL mechanisms.
 *
 * Other than the DbConn wiring, each operation should be self contained, and responsible for
 * performing any required DB commits, error handling and retry behavior.
 */

trait BankAccountWriteOps {

	def makeAccount(customerName: String, customerAddress: String, initBal: BalanceAmount): URIO[DbConn, AcctOpResult[AccountID]]

	def storeBalanceChange(acctID: AccountID, changeAmt: ChangeAmount): URIO[DbConn, AcctOpResult[BalanceChangeSummary]]

}
