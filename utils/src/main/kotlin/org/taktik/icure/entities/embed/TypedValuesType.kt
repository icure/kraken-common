package org.taktik.icure.entities.embed

enum class TypedValuesType {
    BOOLEAN, INTEGER, DOUBLE, STRING, DATE, CLOB, JSON;

    companion object {
        fun fromInt(value: Int): TypedValuesType {
            return TypedValuesType::class.java.enumConstants[value]
        }

        fun fetchAll(): String {
            val all = StringBuilder()
            for (type in entries) {
                all.append(type)
                all.append("\n")
            }
            return all.toString()
        }
    }
}