/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.external

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.DecoratedKotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.external.ExternalKotlinCompilationDescriptor.*
import kotlin.properties.Delegates

@ExternalKotlinTargetApi
interface ExternalKotlinCompilationDescriptor<T : DecoratedExternalKotlinCompilation> {

    /**
     * Scheduled for removal in Kotlin 2.0
     */
    @Deprecated(
        "Renamed to 'CompilationFactory'", level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("CompilationFactory")
    )
    fun interface DecoratedKotlinCompilationFactory<T : DecoratedKotlinCompilation<*>> : CompilationFactory<T>

    fun interface CompilationFactory<T : DecoratedKotlinCompilation<*>> {
        fun create(delegate: DecoratedExternalKotlinCompilation.Delegate): T
    }

    fun interface FriendArtifactResolver<T : DecoratedExternalKotlinCompilation> {
        fun resolveFriendPaths(compilation: T): FileCollection
    }

    fun interface CompilationAssociator<T : DecoratedExternalKotlinCompilation> {
        fun associate(auxiliary: T, main: DecoratedExternalKotlinCompilation)
    }

    val compilationName: String
    val compileTaskName: String?
    val compileAllTaskName: String?
    val defaultSourceSet: KotlinSourceSet

    @Suppress("deprecation_error")
    @Deprecated(
        "Renamed to compilationFactory", level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("compilationFactory")
    )
    val decoratedKotlinCompilationFactory: DecoratedKotlinCompilationFactory<T>
    val compilationFactory: CompilationFactory<T>
    val friendArtifactResolver: FriendArtifactResolver<T>?
    val compilationAssociator: CompilationAssociator<T>?
    val configure: ((T) -> Unit)?
}

@ExternalKotlinTargetApi
fun <T : DecoratedExternalKotlinCompilation> ExternalKotlinCompilationDescriptor(
    configure: ExternalKotlinCompilationDescriptorBuilder<T>.() -> Unit
): ExternalKotlinCompilationDescriptor<T> {
    return ExternalKotlinCompilationDescriptorBuilder<T>().also(configure).run {
        ExternalKotlinCompilationDescriptorImpl(
            compilationName = compilationName,
            compileTaskName = compileTaskName,
            compileAllTaskName = compileAllTaskName,
            defaultSourceSet = defaultSourceSet,
            compilationFactory = compilationFactory ?: decoratedKotlinCompilationFactory,
            friendArtifactResolver = friendArtifactResolver,
            compilationAssociator = compilationAssociator,
            configure = this.configure
        )
    }
}

@ExternalKotlinTargetApi
class ExternalKotlinCompilationDescriptorBuilder<T : DecoratedExternalKotlinCompilation> internal constructor() {
    var compilationName: String by Delegates.notNull()
    var compileTaskName: String? = null
    var compileAllTaskName: String? = null
    var defaultSourceSet: KotlinSourceSet by Delegates.notNull()

    @Suppress("deprecation_error")
    var decoratedKotlinCompilationFactory: DecoratedKotlinCompilationFactory<T> by Delegates.notNull()
    var compilationFactory: CompilationFactory<T>? = null

    var friendArtifactResolver: FriendArtifactResolver<T>? = null
    var compilationAssociator: CompilationAssociator<T>? = null
    var configure: ((T) -> Unit)? = null

    fun configure(action: (T) -> Unit) = apply {
        val configure = this.configure
        if (configure == null) this.configure = action
        else this.configure = { configure(it); action(it) }
    }
}

@ExternalKotlinTargetApi
private data class ExternalKotlinCompilationDescriptorImpl<T : DecoratedExternalKotlinCompilation>(
    override val compilationName: String,
    override val compileTaskName: String?,
    override val compileAllTaskName: String?,
    override val defaultSourceSet: KotlinSourceSet,
    override val compilationFactory: CompilationFactory<T>,
    override val friendArtifactResolver: FriendArtifactResolver<T>?,
    override val compilationAssociator: CompilationAssociator<T>?,
    override val configure: ((T) -> Unit)?
) : ExternalKotlinCompilationDescriptor<T> {
    @Suppress("deprecation_error", "OVERRIDE_DEPRECATION")
    override val decoratedKotlinCompilationFactory: DecoratedKotlinCompilationFactory<T>
        get() = DecoratedKotlinCompilationFactory { compilationFactory.create(it) }
}