package com.appstract.banquo.svc

import com.appstract.banquo.api.bank.AccountOpResultTypes.AccountHistory
import com.appstract.banquo.api.bank.BankScalarTypes.DecodeErrMsg
import com.appstract.banquo.api.bank.{AccountSummary, BalanceChangeSummary, XactInput}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}
import zio.json._

object OurJsonEncoders {
	implicit val acctSumEnc: JsonEncoder[AccountSummary] = DeriveJsonEncoder.gen[AccountSummary]
	implicit val balChgEnc: JsonEncoder[BalanceChangeSummary] = DeriveJsonEncoder.gen[BalanceChangeSummary]

	def encodeAccountSummary(acctSummary : AccountSummary) : String = {
		acctSummary.toJson
	}
	def encodeAccountHistory(acctHistory : AccountHistory) : String = {
		acctHistory.toJson
	}
	def encodeBalanceSummary(balChgSumm : BalanceChangeSummary) : String = {
		balChgSumm.toJson
	}
}

object OurJsonDecoders {
	implicit val xactInputDec: JsonDecoder[XactInput] = DeriveJsonDecoder.gen[XactInput]

	def decodeXactInput(xinTxt : String) : Either[DecodeErrMsg, XactInput] = {
		xinTxt.fromJson[XactInput]
	}

}