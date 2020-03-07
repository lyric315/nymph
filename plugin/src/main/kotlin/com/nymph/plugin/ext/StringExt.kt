package com.nymph.plugin.ext

fun String.toInternalName() = this.replace('.', '/')
fun String.toRegularName() = this.replace('/', '.')