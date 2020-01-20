/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.Logger

import scala.concurrent.ExecutionContext
import scala.concurrent.stm.{TMap, atomic}
import scala.language.higherKinds

object Memoized {

  def memoized[F[_]: Monad, A, B](
    cacheRead: A => F[Option[(B, LocalDateTime)]],
    cacheWrite: (A, (B, LocalDateTime)) => F[Unit],
    ttlSeconds: Long
  )(
    f: A => F[B]
  ): A => F[B] = { args =>
    val now = LocalDateTime.now
    val ttl = LocalDateTime.now.plusSeconds(ttlSeconds)
    cacheRead(args).flatMap {
      case Some((v, d)) if d.isAfter(now) =>
        v.pure[F]
      case _ =>
        f(args).flatMap { z =>
          cacheWrite(args, (z, ttl)).map(_ => z)
        }
    }
  }

  /**
    *  There's something odd about ScalaStm that means if you define the cache in the functinon call like this..
    *
    *  val memoized = Memoized.memoizedWithStm[Future, String, LocalDateTime](TMap[String, (Option[LocalDateTime], LocalDateTime)](), 60 * 60)(_,_)
    *
    *  Instead you must define cache as a val and then pass that in to the function call like this..
    *
    *  val cache: TMap[String, (Option[LocalDateTime], LocalDateTime)] = TMap[String, (Option[LocalDateTime], LocalDateTime)]()
    *  val memoized = Memoized.memoizedWithStm[Future, String, LocalDateTime](cache, 60 * 60)(_,_)
    *
    */
  def memoizedCache[F[_]: Monad, A, B](
    underlyingCache: TMap[A, (B, LocalDateTime)],
    secondsToCache: Long
  )(
    f: A => F[B]
  ): A => F[B] = {

    def read(k: A): F[Option[(B, LocalDateTime)]] =
      atomic { implicit t =>
        underlyingCache.get(k)
      }.pure[F]

    def write(key: A, value: (B, LocalDateTime)): F[Unit] =
      atomic(implicit t => underlyingCache.put(key, value)).map(x => ()).getOrElse(()).pure[F]

    memoized(
      read,
      write,
      secondsToCache
    )(f)
  }

}
