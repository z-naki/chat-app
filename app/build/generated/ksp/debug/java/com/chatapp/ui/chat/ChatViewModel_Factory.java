package com.chatapp.ui.chat;

import androidx.lifecycle.SavedStateHandle;
import com.chatapp.domain.repository.ChatRepository;
import com.chatapp.domain.usecase.SendMessageUseCase;
import com.chatapp.domain.usecase.StreamMessageUseCase;
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
public final class ChatViewModel_Factory implements Factory<ChatViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<ChatRepository> chatRepositoryProvider;

  private final Provider<SendMessageUseCase> sendMessageUseCaseProvider;

  private final Provider<StreamMessageUseCase> streamMessageUseCaseProvider;

  public ChatViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<ChatRepository> chatRepositoryProvider,
      Provider<SendMessageUseCase> sendMessageUseCaseProvider,
      Provider<StreamMessageUseCase> streamMessageUseCaseProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.sendMessageUseCaseProvider = sendMessageUseCaseProvider;
    this.streamMessageUseCaseProvider = streamMessageUseCaseProvider;
  }

  @Override
  public ChatViewModel get() {
    return newInstance(savedStateHandleProvider.get(), chatRepositoryProvider.get(), sendMessageUseCaseProvider.get(), streamMessageUseCaseProvider.get());
  }

  public static ChatViewModel_Factory create(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<ChatRepository> chatRepositoryProvider,
      Provider<SendMessageUseCase> sendMessageUseCaseProvider,
      Provider<StreamMessageUseCase> streamMessageUseCaseProvider) {
    return new ChatViewModel_Factory(savedStateHandleProvider, chatRepositoryProvider, sendMessageUseCaseProvider, streamMessageUseCaseProvider);
  }

  public static ChatViewModel newInstance(SavedStateHandle savedStateHandle,
      ChatRepository chatRepository, SendMessageUseCase sendMessageUseCase,
      StreamMessageUseCase streamMessageUseCase) {
    return new ChatViewModel(savedStateHandle, chatRepository, sendMessageUseCase, streamMessageUseCase);
  }
}
