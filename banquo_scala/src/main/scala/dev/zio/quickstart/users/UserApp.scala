package dev.zio.quickstart.users

import zio._
import zio.http._
import zio.json._

/** An http app that:
 *   - Accepts a `Request` and returns a `Response`
 *   - May fail with type of `Throwable`
 *   - Uses a `UserRepo` as the environment
 *
 *   From windows bash, can test user create with:
 *
 *    curl.exe -d '{"name": "Sally", "age": 49999}' -X POST http://localhost:8484/users
 *
 *
 */
object UserApp {
	def apply(): Http[UserRepo, Throwable, Request, Response] =
		Http.collectZIO[Request] {
			// POST /users -d '{"name": "John", "age": 35}'
			case req @ (Method.POST -> Root / "users") => {
				val bodyTxt = req.body.asString.debug("UserApp.POST with bodyTxt")

				val outTask: ZIO[UserRepo, Throwable, Response] = for {
					inputText <- bodyTxt
					u <- bodyTxt.map(_.fromJson[User])
					r <- u match {
						case Left(e) =>
							ZIO
									.debug(s"Failed to parse input [$inputText], error: $e")
									.as(
										Response.text(e).withStatus(Status.BadRequest)
									)
						case Right(u) =>
							UserRepo
									.register(u)
									.map(id => Response.text(id))
					}
					dr = r
				} yield r
				outTask.tap(resp => ZIO.log(s"UserApp.POST response status: [${resp.status}], body [${resp.body}]"))
			}


			// GET /users/:id
			case Method.GET -> Root / "users" / id =>
				UserRepo
						.lookup(id)
						.map {
							case Some(user) =>
								Response.json(user.toJson)
							case None =>
								Response.status(Status.NotFound)
						}
			// GET /users
			case Method.GET -> Root / "users" =>
				UserRepo.users.map(response => Response.json(response.toJson))
		}

}
