package dev.tomerklein.holocron.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.tomerklein.holocron.data.DeliveryLogDao
import dev.tomerklein.holocron.data.DestinationDao
import dev.tomerklein.holocron.data.HolocronDatabase
import dev.tomerklein.holocron.data.RuleDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HolocronDatabase =
        Room.databaseBuilder(context, HolocronDatabase::class.java, "holocron.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideRuleDao(db: HolocronDatabase): RuleDao = db.ruleDao()
    @Provides fun provideDestinationDao(db: HolocronDatabase): DestinationDao = db.destinationDao()
    @Provides fun provideDeliveryLogDao(db: HolocronDatabase): DeliveryLogDao = db.deliveryLogDao()
}
