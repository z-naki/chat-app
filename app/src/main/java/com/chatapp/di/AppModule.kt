package com.chatapp.di

import android.content.Context
import androidx.room.Room
import com.chatapp.data.local.db.AppDatabase
import com.chatapp.data.local.db.dao.ConversationDao
import com.chatapp.data.local.db.dao.MessageDao
import com.chatapp.data.local.prefs.SecurePrefs
import com.chatapp.data.remote.provider.ProviderRouter
import com.chatapp.data.remote.provider.deepseek.DeepSeekProvider
import com.chatapp.data.repository.ChatRepositoryImpl
import com.chatapp.data.repository.SettingsRepositoryImpl
import com.chatapp.domain.repository.ChatRepository
import com.chatapp.domain.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "chat_app.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideConversationDao(db: AppDatabase): ConversationDao = db.conversationDao()

    @Provides
    fun provideMessageDao(db: AppDatabase): MessageDao = db.messageDao()

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS) // no timeout for streaming
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.NONE
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideProviderRouter(
        securePrefs: SecurePrefs,
        deepSeekProvider: DeepSeekProvider
    ): ProviderRouter {
        return ProviderRouter(securePrefs).apply {
            register(deepSeekProvider)
        }
    }

    @Provides
    @Singleton
    fun provideChatRepository(impl: ChatRepositoryImpl): ChatRepository = impl

    @Provides
    @Singleton
    fun provideSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository = impl
}
