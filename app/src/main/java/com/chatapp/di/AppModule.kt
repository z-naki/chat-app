package com.chatapp.di

import android.content.Context
import androidx.room.Room
import com.chatapp.data.local.db.AppDatabase
import com.chatapp.data.local.db.dao.ConversationDao
import com.chatapp.data.local.db.dao.MessageDao
import com.chatapp.data.local.prefs.SecurePrefs
import com.chatapp.data.remote.provider.CustomProvider1
import com.chatapp.data.remote.provider.CustomProvider2
import com.chatapp.data.remote.provider.CustomProvider3
import com.chatapp.data.remote.provider.ProviderRouter
import com.chatapp.data.remote.provider.anthropic.AnthropicProvider
import com.chatapp.data.remote.provider.deepseek.DeepSeekProvider
import com.chatapp.data.remote.provider.gemini.GeminiProvider
import com.chatapp.data.remote.provider.openai.MoonshotProvider
import com.chatapp.data.remote.provider.openai.OpenAiProvider
import com.chatapp.data.remote.provider.openai.QwenProvider
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
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
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
            // Certificate pinning — add provider cert hashes here
            .certificatePinner(CertificatePinner.Builder()
                // .add("api.deepseek.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
                .build())
            .build()
    }

    @Provides
    @Singleton
    fun provideProviderRouter(
        securePrefs: SecurePrefs,
        deepSeekProvider: DeepSeekProvider,
        openAiProvider: OpenAiProvider,
        anthropicProvider: AnthropicProvider,
        geminiProvider: GeminiProvider,
        moonshotProvider: MoonshotProvider,
        qwenProvider: QwenProvider,
        custom1: CustomProvider1,
        custom2: CustomProvider2,
        custom3: CustomProvider3
    ): ProviderRouter {
        return ProviderRouter(securePrefs).apply {
            register(deepSeekProvider)
            register(openAiProvider)
            register(anthropicProvider)
            register(geminiProvider)
            register(moonshotProvider)
            register(qwenProvider)
            register(custom1)
            register(custom2)
            register(custom3)
        }
    }

    @Provides
    @Singleton
    fun provideChatRepository(impl: ChatRepositoryImpl): ChatRepository = impl

    @Provides
    @Singleton
    fun provideSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository = impl
}
