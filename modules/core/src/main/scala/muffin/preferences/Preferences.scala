package muffin.preferences

import muffin.predef.*

trait Preferences[F[_]] {
  def getUserPreferences(userId: UserId): F[List[Preference]]

  def getUserPreferences(userId: UserId, category: String): F[List[Preference]]

  def getUserPreference(userId: UserId, category: String, name: String): F[Preference]

  def saveUserPreference(userId: UserId, preferences: List[Preference]): F[Unit]

  def deleteUserPreference(userId: UserId, preferences: List[Preference]): F[Unit]
}
