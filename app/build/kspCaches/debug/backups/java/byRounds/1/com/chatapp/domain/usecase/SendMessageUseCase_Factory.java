package com.chatapp.domain.usecase;

import com.chatapp.domain.repository.ChatRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class SendMessageUseCase_Factory implements Factory<SendMessageUseCase> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  public SendMessageUseCase_Factory(Provider<ChatRepository> chatRepositoryProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
  }

  @Override
  public SendMessageUseCase get() {
    return newInstance(chatRepositoryProvider.get());
  }

  public static SendMessageUseCase_Factory create(Provider<ChatRepository> chatRepositoryProvider) {
    return new SendMessageUseCase_Factory(chatRepositoryProvider);
  }

  public static SendMessageUseCase newInstance(ChatRepository chatRepository) {
    return new SendMessageUseCase(chatRepository);
  }
}
