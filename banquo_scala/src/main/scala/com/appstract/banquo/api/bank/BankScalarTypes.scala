package com.appstract.banquo.api.bank
import java.sql.{Timestamp => JsqlTimestamp}

object BankScalarTypes {
	type AccountID = String
	type CustomerName = String
	type CustomerAddress = String
	type DbTimestamp = JsqlTimestamp

	type BalanceChangeID = Long
	type XactDescription = String

	type ChangeAmount = BigDecimal
	type BalanceAmount = BigDecimal

	type ChangeFlavor = String // TODO:  This is an SQL enum, which we might map into a strict Scala type.

	type DecodeErrMsg = String
}