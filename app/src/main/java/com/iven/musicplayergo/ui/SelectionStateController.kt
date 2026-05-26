package com.acerem.musicplayerar.ui

import java.util.LinkedHashSet

class SelectionStateController<K> {

    private val selectedKeys = LinkedHashSet<K>()

    fun hasSelection(): Boolean = selectedKeys.isNotEmpty()

    fun selectionCount(): Int = selectedKeys.size

    fun isSelected(key: K): Boolean = selectedKeys.contains(key)

    fun getSelectedKeys(): Set<K> = selectedKeys.toSet()

    fun toggle(key: K): Boolean {
        if (!selectedKeys.add(key)) {
            selectedKeys.remove(key)
            return false
        }
        return true
    }

    fun select(key: K) {
        selectedKeys.add(key)
    }

    fun clear() {
        selectedKeys.clear()
    }

    fun selectAll(keys: Collection<K>) {
        selectedKeys.clear()
        selectedKeys.addAll(keys)
    }
}
