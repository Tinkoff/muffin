package muffin

import muffin.internal.dsl.*

object dsl extends MessageSyntax with AppResponseSyntax with RouterSyntax with DialogSyntax
