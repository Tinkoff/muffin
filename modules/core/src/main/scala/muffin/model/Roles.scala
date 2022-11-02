package muffin.model

import muffin.api.*

case class RoleInfo(
    id: String,
    name: String,
    displayName: String,
    description: String,
    permissions: List[String],
    schemeManaged: Boolean
)
