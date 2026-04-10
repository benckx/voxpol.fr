package fr.voxpol.webapp.utils

import org.koin.core.component.KoinComponent
import org.koin.java.KoinJavaComponent.inject
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Property delegate for lazy Koin dependency injection.
 * Does not require extending KoinComponent — safe to use at the file (top-level) scope.
 * Usage: private val service by koin<MyService>()
 */
inline fun <reified T> koin(): ReadOnlyProperty<Any?, T> = KoinDelegate { getKoinInstance<T>() }

inline fun <reified T> getKoinInstance(): T {
    val component = object : KoinComponent {
        val value: T by inject(T::class.java)
    }
    return component.value
}

class KoinDelegate<T>(initializer: () -> T) : ReadOnlyProperty<Any?, T> {
    private val lazy = lazy(initializer)
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = lazy.value
}
