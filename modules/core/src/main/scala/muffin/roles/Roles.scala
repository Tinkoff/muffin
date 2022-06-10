package muffin.roles


import muffin.predef.*


trait Roles[F[_]] {
  def getAllRoles: F[List[RoleInfo]]

  def getRoleById(id: String): F[RoleInfo]

  def getRoleByName(name: String): F[RoleInfo]

  def updateRole(id: String, permissions: List[String]): F[RoleInfo]

  def getRoles(names: List[String]): F[List[RoleInfo]]
}
