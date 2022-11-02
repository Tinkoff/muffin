package muffin

import muffin.internal.dsl.{AppResponseSyntax, MessageSyntax, RouterSyntax}

object dsl extends MessageSyntax with AppResponseSyntax with RouterSyntax
