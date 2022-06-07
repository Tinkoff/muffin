package muffin.preferences


import muffin.predef.*

case class Preference(
                       user_id: UserId,
                       category: String,
                       name: String,
                       value: String
                     )