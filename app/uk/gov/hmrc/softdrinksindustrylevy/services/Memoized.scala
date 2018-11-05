/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.softdrinksindustrylevy.services

import java.time.LocalDateTime

import cats.Monad
import cats.implicits._
import play.api.Configuration
import play.api.Mode.Mode
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.softdrinksindustrylevy.connectors.DesHelpers
import uk.gov.hmrc.softdrinksindustrylevy.models.{Subscription, json}

import scala.concurrent.stm.{TMap, atomic}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

trait Memoized {

  def memoized[F[_] : Monad,A,B](
    f: A => F[B],
    cacheRead: A => F[Option[(B,LocalDateTime)]],
    cacheWrite: (A, (B,LocalDateTime)) => F[Unit],
    ttl: LocalDateTime = LocalDateTime.now().plusHours(1)
  ): A => F[B] = { args =>
    cacheRead(args).flatMap {
      case Some((v,d)) if d.isBefore(ttl) => {
        v.pure[F]
      }
      case None => {
        f(args).flatMap { z =>
          cacheWrite(args, (z, LocalDateTime.now())).map(_ => z)
        }
      }
    }
  }

  protected trait Cache[F[_],A,B] { // F = Future B = Option[Subscription] A = String
    def fetch(key: A): F[B]
    def read(key: A): F[Option[(B,LocalDateTime)]] // Future[Option[(Option[Subscription], LocalDateTime)]]
    def write(key: A, value: (B,LocalDateTime)): F[Unit]
    def ttl: LocalDateTime
  }

//  def subscriptionsCache: Cache[Future, String, Option[Subscription]]
}

class MemoizedSubscriptions(
  httpClient: HttpClient,
  val mode: Mode,
  val runModeConfiguration: Configuration
) extends Memoized with DesHelpers {

  val cache: TMap[String, (Option[Subscription], LocalDateTime)] = TMap[String, (Option[Subscription], LocalDateTime)]()
  val http: HttpClient = httpClient

  def getSubscription(url: String)
    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Subscription]] = {

    import json.des.get._

    val subscriptionsCache = new Cache[Future, String, Option[Subscription]] {

      override def fetch(key: String): Future[Option[Subscription]] =
        http.GET[Option[Subscription]](url)(implicitly, addHeaders, ec)

      override def read(key: String): Future[Option[(Option[Subscription], LocalDateTime)]] = {
        println(s"QQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQ read $key")
        Future(atomic {implicit t => cache.get(key)})
      }

      override def write(key: String, value: (Option[Subscription], LocalDateTime)):Future[Unit] = {
        println(s"PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP write $key")
        Future(atomic(implicit t => cache.put(key, value)).map(_ => ()))
      }

//      override def fetch(key: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Subscription]] =
//        http.GET[Option[Subscription]](key)(implicitly, addHeaders, ec)
//
//      override def read(key: String)(implicit ec: ExecutionContext) =
//        Future(atomic {implicit t => cache.get(key)})
//
//      override def write(key: String, value: (Option[Subscription], LocalDateTime))(implicit ec: ExecutionContext): Future[Unit] =
//        Future(atomic(implicit t => cache.put(key, value)).map(_ => ()))

      override def ttl: LocalDateTime = LocalDateTime.now().plusHours(1)
    }


    //    import json.des.get._

//    implicit val reads = json.des.get.

    memoized[Future, String, Option[Subscription]](
      subscriptionsCache.fetch,
      subscriptionsCache.read,
      subscriptionsCache.write
    ).apply(url)
  }

//  implicit def readOptionOf[P](implicit rds: HttpReads[P]): HttpReads[Option[P]] = new HttpReads[Option[P]] {
//    def read(method: String, url: String, response: HttpResponse): Option[P] = response.status match {
//      case 204 | 404 | 503 => None
//      case _ => Some(rds.read(method, url, response))
//    }
//  }

}



