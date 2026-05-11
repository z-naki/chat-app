package com.chatapp.data.repository;

import com.chatapp.data.local.db.dao.ConversationDao;
import com.chatapp.data.local.db.dao.MessageDao;
import com.chatapp.data.remote.provider.ProviderRouter;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class ChatRepositoryImpl_Factory implements Factory<ChatRepositoryImpl> {
  private final Provider<ConversationDao> conversationDaoProvider;

  private final Provider<MessageDao> messageDaoProvider;

  private final Provider<ProviderRouter> providerRouterProvider;

  public ChatRepositoryImpl_Factory(Provider<ConversationDao> conversationDaoProvider,
      Provider<MessageDao> messageDaoProvider, Provider<ProviderRouter> providerRouterProvider) {
    this.conversationDaoProvider = conversationDaoProvider;
    this.messageDaoProvider = messageDaoProvider;
    this.providerRouterProvider = providerRouterProvider;
  }

  @Override
  public ChatRepositoryImpl get() {
    return newInstance(conversationDaoProvider.get(), messageDaoProvider.get(), providerRouterProvider.get());
  }

  public static ChatRepositoryImpl_Factory create(Provider<ConversationDao> conversationDaoProvider,
      Provider<MessageDao> messageDaoProvider, Provider<ProviderRouter> providerRouterProvider) {
    return new ChatRepositoryImpl_Factory(conversationDaoProvider, messageDaoProvider, providerRouterProvider);
  }

  public static ChatRepositoryImpl newInstance(ConversationDao conversationDao,
      MessageDao messageDao, ProviderRouter providerRouter) {
    return new ChatRepositoryImpl(conversationDao, messageDao, providerRouter);
  }
}
