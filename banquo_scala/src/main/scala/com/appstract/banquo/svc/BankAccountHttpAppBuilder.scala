package com.appstract.banquo.svc

import com.appstract.banquo.api.{BankAccountReadOps, BankAccountWriteOps, DbConn}
import com.appstract.banquo.impl.bank.BankAccountWriteOpsImpl
import zio._
import zio.http._
import zio.json._

class BankAccountHttpAppBuilder {
	def makeHttpApp(accountWriteOps: => BankAccountWriteOps, accountReadOps: => BankAccountReadOps): Http[DbConn, Nothing, Request, Response] = {
		Http.collectZIO[Request] {

			case Method.GET -> Root / "account" / acct_id => {
/*
200 OK: Returns the balance and user details of the specified bank account in JSON format.
404 Not Found: If the account does not exist.
 */
				ZIO.succeed(Response.text("bad request").withStatus(Status.BadRequest))
			}
			case Method.GET -> Root / "transaction" / "history" / acct_id => {
/*
200 OK: Returns the list of transaction history for the specified bank account in JSON format.
404 Not Found: If the account does not exist.
 */
				ZIO.succeed(Response.text("bad request").withStatus(Status.BadRequest))
			}
			case req@(Method.POST -> Root / "transaction") => {
				/*
				201 Created: If the transaction is successful, it returns the transaction details in JSON format.
				400 Bad Request: If the request body is invalid.
				404 Not Found: If the account does not exist.
				422 Unprocessable Entity: If the transaction fails due to insufficient funds or other reasons.
				*/
				val bodyTxt = req.body.asString.debug("POST /transaction with bodyTxt")
				ZIO.succeed(Response.text("bad request").withStatus(Status.BadRequest))
			}
		}
	}
}
