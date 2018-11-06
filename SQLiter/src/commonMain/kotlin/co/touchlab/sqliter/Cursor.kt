package co.touchlab.sqliter

/**
 * Simplified Cursor implementation. Forward-only traversal.
 */
interface Cursor {
    fun next(): Boolean
    fun isNull(index: Int): Boolean
    fun getString(index: Int): String
    fun getLong(index: Int): Long
    fun getBytes(index: Int): ByteArray
    fun getDouble(index: Int): Double
    fun getType(index: Int):FieldType
    val columnCount: Int
    fun columnName(index: Int): String
    fun close()
    val columnNames: Map<String, Int>
}

enum class FieldType(val nativeCode: Int) {
    INTEGER(1), FLOAT(2), BLOB(4), NULL(5), TEXT(3);

    companion object {
        fun forCode(nativeCode: Int):FieldType{
            FieldType.values().forEach {
                if(it.nativeCode == nativeCode)
                    return it
            }
            throw IllegalArgumentException("Native code not found $nativeCode")
        }
    }
}

fun Cursor.getStringOrNull(index: Int): String?{
    return if(isNull(index))
        null
    else
        getString(index)
}

fun Cursor.getLongOrNull(index: Int): Long?{
    return if(isNull(index))
        null
    else
        getLong(index)
}

fun Cursor.getBytesOrNull(index: Int): ByteArray?{
    return if(isNull(index))
        null
    else
        getBytes(index)
}

fun Cursor.getDoubleOrNull(index: Int): Double?{
    return if(isNull(index))
        null
    else
        getDouble(index)
}

fun Cursor.forLong():Long{
    next()
    val result = getLong(0)
    close()
    return result
}

fun Cursor.iterator():CursorIterator = CursorIterator(this)

class Row{
    val values = mutableListOf<Pair<FieldType, Any?>>()
}

class CursorIterator(private val cursor: Cursor):Iterator<Row> {
    var hadNext = cursor.next()

    override fun hasNext(): Boolean = hadNext

    override fun next(): Row {
        val result = Row()
        for(i in 0 until cursor.columnCount){
            val type = cursor.getType(i)
            val value:Any? = when(type){
                FieldType.BLOB -> cursor.getBytes(i)
                FieldType.FLOAT -> cursor.getDouble(i)
                FieldType.INTEGER -> cursor.getLong(i)
                FieldType.NULL -> null
                FieldType.TEXT -> cursor.getString(i)
            }

            result.values.add(Pair(type, value))
        }

        hadNext = cursor.next()

        return result
    }
}
