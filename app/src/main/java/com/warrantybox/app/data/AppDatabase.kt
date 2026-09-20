package com.warrantybox.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ProductEntity::class, CategoryEntity::class, DocumentEntity::class, RepairEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): WarrantyDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }
        }

        private fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "warrantybox.db")
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val warrantyDao: WarrantyDao = get(context).dao()
                            val defaults = listOf(
                                "Tecnologia", "Gaming", "Eletrodomésticos", "Automóvel",
                                "Casa", "Móveis", "Ferramentas", "Roupa", "Outros"
                            )
                            defaults.forEach { name ->
                                warrantyDao.insertCategory(CategoryEntity(name = name))
                            }
                        }
                    }
                })
                .build()
    }
}
