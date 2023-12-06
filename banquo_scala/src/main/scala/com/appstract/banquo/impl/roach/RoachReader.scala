package com.appstract.banquo.impl.roach

import com.appstract.banquo.api.{AccountDetails, BalanceChange}
import com.appstract.banquo.api.BankScalarTypes.AccountId

trait RoachReader {
	def selectLastBalanceChange(acctId : AccountId) : Either[DbError,BalanceChange] = ???

	// TODO: AllBalanceChanges should be some kind of stream or paged result.
	def selectAllBalanceChanges(acctId : AccountId) : Either[DbError, Iterable[BalanceChange]] = ???

	def selectAccountDetails(acctId : AccountId) : Either[DbError, AccountDetails] = ???
}

