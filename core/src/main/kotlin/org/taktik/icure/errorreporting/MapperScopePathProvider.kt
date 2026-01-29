package org.taktik.icure.errorreporting

interface MapperScopePathProvider {
	fun getScopePathFor(rootName: String): ScopePath?

	object Never : MapperScopePathProvider {
		override fun getScopePathFor(rootName: String): ScopePath? = null
	}

	object Default : MapperScopePathProvider {
		override fun getScopePathFor(rootName: String): ScopePath = ScopePathImpl(
			ArrayList<Any>(50).also { it.addLast(rootName) },
		)
	}
}