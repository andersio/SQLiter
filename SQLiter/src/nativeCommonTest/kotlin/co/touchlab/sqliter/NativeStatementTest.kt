package co.touchlab.sqliter

import kotlin.test.*

class NativeStatementTest {

    @BeforeEach
    fun before() {
        deleteDatabase("testdb")
    }

    @AfterEach
    fun after() {
        deleteDatabase("testdb")
    }

    @Test
    fun insertStatement() {
        basicTestDb {
            val connection = it.createConnection()
            val statement = connection.createStatement("INSERT INTO test VALUES (?, ?, ?, ?)")
            for (i in 0 until 50) {
                statement.bindLong(1, i.toLong())
                statement.bindString(2, "Hilo $i")
                if (i % 2 == 0)
                    statement.bindString(3, null)
                else
                    statement.bindString(3, "asdf jfasdf $i fflkajsdf $i")
                statement.bindString(4, "WWWWW QWER jfasdf $i fflkajsdf $i")
                statement.executeInsert()
                statement.reset()
            }
            statement.finalize()

            connection.withStatement("select str from test") {
                val query = it.query()
                query.next()
                assertTrue(query.getString(query.columnNames["str"]!!).startsWith("Hilo"))
            }
            connection.close()
        }
    }

    @Test
    fun updateStatement() {
        basicTestDb {
            val connection = it.createConnection()
            val statement = connection.createStatement("INSERT INTO test VALUES (?, ?, ?, ?)")
            for (i in 0 until 50) {
                statement.bindLong(1, i.toLong())
                statement.bindString(2, "Hilo $i")
                if (i % 2 == 0)
                    statement.bindString(3, null)
                else
                    statement.bindString(3, "asdf jfasdf $i fflkajsdf $i")
                statement.bindString(4, "WWWWW QWER jfasdf $i fflkajsdf $i")
                statement.executeInsert()
                statement.reset()
            }
            statement.finalize()

            connection.withStatement("update test set str = ?") {
                it.bindString(1, "asdf")
                it.executeUpdateDelete()
            }

            connection.withStatement("select str from test") {
                val query = it.query()
                query.next()
                assertFalse(query.getString(query.columnNames["str"]!!).startsWith("Hilo"))
                assertTrue(query.getString(query.columnNames["str"]!!).startsWith("asdf"))
            }
            connection.close()
        }
    }


    @Test
    fun updateCountResult() {
        basicTestDb(TWO_COL) {
            val connection = it.createConnection()
            connection.withTransaction {
                it.withStatement("insert into test(num, str)values(?,?)") {
                    it.bindLong(1, 1)
                    it.bindString(2, "asdf")
                    assertTrue(it.executeInsert() > 0)
                    it.reset()
                    it.bindLong(1, 2)
                    it.bindString(2, "rrr")
                    it.executeInsert()
                }
            }

            connection.withTransaction {
                it.withStatement("update test set str = ?") {
                    it.bindString(1, "qwert")
                    assertEquals(2, it.executeUpdateDelete())
                }
            }

            connection.close()
        }
    }

    @Test
    fun statementIndexIssues() {
        basicTestDb(TWO_COL) {
            it.withConnection {
                it.withStatement("insert into test(num, str)values(?,?)") {
                    assertFails { it.bindString(0, "asdf") }
                    assertFails { it.bindString(3, "asdf") }

                    //Still works?
                    it.bindLong(1, 123)
                    it.bindString(2, "asdf")
                    assertTrue(it.executeInsert() > 0)
                }
            }
        }
    }

    @Test
    fun failNotNullNull() {
        basicTestDb(TWO_COL) {
            it.withConnection {
                it.withStatement("insert into test(num, str)values(?,?)") {
                    it.bindLong(1, 333)
                    it.bindString(2, null)
                    assertFails { it.executeInsert() }
                }
            }
        }
    }

    @Test
    fun failBadFormat() {
        basicTestDb(TWO_COL) {
            it.withConnection {
                assertFails { it.createStatement("insert into test(num, str)values(?,?") }
            }
        }
    }

    @Test
    fun failPartialBind() {
        basicTestDb(TWO_COL) {
            it.withConnection {
                it.withStatement("insert into test(num, str)values(?,?)") {
                    it.bindLong(1, 21)
                    it.bindString(2, "asdf")
                    it.executeInsert()
                    it.bindLong(1, 44)
                    assertFails { it.executeInsert() }
                }
            }
        }
    }
}

