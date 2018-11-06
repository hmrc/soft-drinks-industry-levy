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
import uk.gov.hmrc.softdrinksindustrylevy.models.Subscription

import scala.concurrent.stm.{TMap, atomic}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

trait Memoized {

  def memoized[F[_] : Monad,A,B](
    f: A => F[B],
    cacheRead: A => F[Option[(B,LocalDateTime)]],
    cacheWrite: (A, (B,LocalDateTime)) => F[Unit],
    ttl: LocalDateTime
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

  protected trait Cache[F[_],A,B] {
    def read(key: A)(implicit ec: ExecutionContext): F[Option[(B,LocalDateTime)]]
    def write(key: A, value: (B,LocalDateTime))(implicit ec: ExecutionContext): F[Unit]
    def ttl: LocalDateTime
  }

  def subscriptionsCache: Cache[Future, String, Option[Subscription]]

}

class MemoizedSubscriptions extends Memoized {

  val cache: TMap[String, (Option[Subscription], LocalDateTime)] = TMap[String, (Option[Subscription], LocalDateTime)]()

  val subscriptionsCache = new Cache[Future, String, Option[Subscription]] {

    override def read(key: String)(implicit ec: ExecutionContext): Future[Option[(Option[Subscription], LocalDateTime)]] = {
      println(s"################################### read $key")
      Future(atomic {implicit t => cache.get(key)})
    }

    override def write(key: String, value: (Option[Subscription], LocalDateTime))(implicit ec: ExecutionContext): Future[Unit] = {
      println(s"################################### write $key")
      Future(atomic(implicit t => cache.put(key, value)).map(_ => ()))
    }

    override def ttl: LocalDateTime = LocalDateTime.now().plusHours(1)
  }


  def getSubscription(f: String => Future[Option[Subscription]], url: String) // TODO investigate using type arguments
//  def getSubscription[F[_] : Monad,A,B](f: A => F[B], url: A)
    (implicit ec: ExecutionContext): Future[Option[Subscription]] = { // TODO see if we can get rid of the ec

    memoized(
      f,
      subscriptionsCache.read,
      subscriptionsCache.write,
      subscriptionsCache.ttl
    ).apply(url)
  }

}



