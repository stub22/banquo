package com.appstract.banquo.svc

import com.appstract.banquo.api.{BankAccountReadOps, BankAccountWriteOps, DbConn}
import com.appstract.banquo.impl.bank.BankAccountWriteOpsImpl
import zio._
import zio.http._
import zio.json._

class BankAccountHttpAppBuilder(accountWriteOps: => BankAccountWriteOps, accountReadOps: => BankAccountReadOps) {
	def makeHttpApp = {
		Http.collectZIO[Request] {

			case req@(Method.POST -> Root / "transaction") => {
				val bodyTxt = req.body.asString.debug("POST /transaction with bodyTxt")
				ZIO.succeed(Response.text("bad request").withStatus(Status.BadRequest))
			}
		}
	}
}
