package com.chatapp.ui.conversationlist;

import com.chatapp.domain.usecase.CreateConversationUseCase;
import com.chatapp.domain.usecase.DeleteConversationUseCase;
import com.chatapp.domain.usecase.GetConversationsUseCase;
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
public final class ConversationListViewModel_Factory implements Factory<ConversationListViewModel> {
  private final Provider<GetConversationsUseCase> getConversationsUseCaseProvider;

  private final Provider<CreateConversationUseCase> createConversationUseCaseProvider;

  private final Provider<DeleteConversationUseCase> deleteConversationUseCaseProvider;

  public ConversationListViewModel_Factory(
      Provider<GetConversationsUseCase> getConversationsUseCaseProvider,
      Provider<CreateConversationUseCase> createConversationUseCaseProvider,
      Provider<DeleteConversationUseCase> deleteConversationUseCaseProvider) {
    this.getConversationsUseCaseProvider = getConversationsUseCaseProvider;
    this.createConversationUseCaseProvider = createConversationUseCaseProvider;
    this.deleteConversationUseCaseProvider = deleteConversationUseCaseProvider;
  }

  @Override
  public ConversationListViewModel get() {
    return newInstance(getConversationsUseCaseProvider.get(), createConversationUseCaseProvider.get(), deleteConversationUseCaseProvider.get());
  }

  public static ConversationListViewModel_Factory create(
      Provider<GetConversationsUseCase> getConversationsUseCaseProvider,
      Provider<CreateConversationUseCase> createConversationUseCaseProvider,
      Provider<DeleteConversationUseCase> deleteConversationUseCaseProvider) {
    return new ConversationListViewModel_Factory(getConversationsUseCaseProvider, createConversationUseCaseProvider, deleteConversationUseCaseProvider);
  }

  public static ConversationListViewModel newInstance(
      GetConversationsUseCase getConversationsUseCase,
      CreateConversationUseCase createConversationUseCase,
      DeleteConversationUseCase deleteConversationUseCase) {
    return new ConversationListViewModel(getConversationsUseCase, createConversationUseCase, deleteConversationUseCase);
  }
}
