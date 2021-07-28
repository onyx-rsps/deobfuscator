package dev.onyx.deobfuscator.util

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.ref.ReferenceQueue
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface WeakReference<T> {
    val reference: T?
    fun clear()
}

interface WeakKeyedReference<K, V> : WeakReference<K> {
    val map: MutableMap<WeakKeyedReference<K, V>, V>
}

open class KotlinWeakReference<T>(value: T) : WeakReference<T> {

    internal val queue = ReferenceQueue<T>()

    private val wrappedWeakReference = java.lang.ref.WeakReference(value, queue)

    override val reference: T? get() = wrappedWeakReference.get()

    override fun clear() {
        wrappedWeakReference.clear()
    }
}

fun <T, V> mixin(): PropertyMixin<T, V> {
    return PropertyMixin()
}

fun <T, V> nullableMixin(): NullablePropertyMixin<T, V> {
    return NullablePropertyMixin()
}

@Suppress("BlockingMethodInNonBlockingContext", "EXPERIMENTAL_API_USAGE")
class KotlinWeakKeyedReference<K, V>(
    key: K,
    override val map: MutableMap<WeakKeyedReference<K, V>, V>
) : KotlinWeakReference<K>(key), WeakKeyedReference<K, V> {

    init {
        GlobalScope.launch {
            queue.remove()
            map.remove(this@KotlinWeakKeyedReference)
        }
    }
}

private fun <K, V, T : WeakReference<K>> MutableMap<T, V>.findWeakReferenceForKey(key: K): T? {
    for((currentKey, _) in this) {
        if(currentKey.reference == key) {
            return currentKey
        }
    }

    return null
}

class PropertyMixin<T, V> : ReadWriteProperty<V, T> {

    private val map = mutableMapOf<WeakKeyedReference<Any, T>, T>()

    override fun getValue(thisRef: V, property: KProperty<*>): T = thisRef?.let {
        map[map.findWeakReferenceForKey(thisRef)]
    } ?: throw UninitializedPropertyAccessException("Mixin property has not been initialized.")

    override fun setValue(thisRef: V, property: KProperty<*>, value: T) {
        val key: WeakKeyedReference<Any, T> = thisRef?.let {
            map.findWeakReferenceForKey(thisRef) ?: KotlinWeakKeyedReference(thisRef, map)
        } ?: return

        map[key] = value
    }

}

class NullablePropertyMixin<T, V> : ReadWriteProperty<V, T?> {

    private val map = mutableMapOf<WeakKeyedReference<Any, T>, T>()

    override fun getValue(thisRef: V, property: KProperty<*>): T? = thisRef?.let {
        map[map.findWeakReferenceForKey(thisRef)]
    } ?: throw UninitializedPropertyAccessException("Mixin property has not been initialized.")

    override fun setValue(thisRef: V, property: KProperty<*>, value: T?) {
        val key: WeakKeyedReference<Any, T> = thisRef?.let {
            map.findWeakReferenceForKey(thisRef) ?: KotlinWeakKeyedReference(thisRef, map)
        } ?: return

        map[key] = value!!
    }
}