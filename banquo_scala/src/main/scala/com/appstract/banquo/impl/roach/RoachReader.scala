package com.appstract.banquo.impl.roach

import com.appstract.banquo.api.{AccountDetails, BalanceChange, DbConn}
import com.appstract.banquo.api.BankScalarTypes.AccountId
import com.appstract.banquo.api.DbResultTypes.DbResult
import zio.URIO



trait RoachReader {

	def selectLastBalanceChange(acctId : AccountId) : URIO[DbConn, DbResult[BalanceChange]] = ???

	// TODO: AllBalanceChanges should be some kind of paged result set, or stream
	def selectAllBalanceChanges(acctId : AccountId) : URIO[DbConn, DbResult[Iterable[BalanceChange]]] = ???

	val SELECT_ACCT_DETAILS = "SELECT acct_id, cust_name, cust_address, acct_create_time"
	def selectAccountDetails(acctId : AccountId) : URIO[DbConn, DbResult[AccountDetails]] = ???
}

