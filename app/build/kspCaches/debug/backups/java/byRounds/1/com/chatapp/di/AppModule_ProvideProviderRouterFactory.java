package com.chatapp.di;

import com.chatapp.data.local.prefs.SecurePrefs;
import com.chatapp.data.remote.provider.ProviderRouter;
import com.chatapp.data.remote.provider.deepseek.DeepSeekProvider;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideProviderRouterFactory implements Factory<ProviderRouter> {
  private final Provider<SecurePrefs> securePrefsProvider;

  private final Provider<DeepSeekProvider> deepSeekProvider;

  public AppModule_ProvideProviderRouterFactory(Provider<SecurePrefs> securePrefsProvider,
      Provider<DeepSeekProvider> deepSeekProvider) {
    this.securePrefsProvider = securePrefsProvider;
    this.deepSeekProvider = deepSeekProvider;
  }

  @Override
  public ProviderRouter get() {
    return provideProviderRouter(securePrefsProvider.get(), deepSeekProvider.get());
  }

  public static AppModule_ProvideProviderRouterFactory create(
      Provider<SecurePrefs> securePrefsProvider, Provider<DeepSeekProvider> deepSeekProvider) {
    return new AppModule_ProvideProviderRouterFactory(securePrefsProvider, deepSeekProvider);
  }

  public static ProviderRouter provideProviderRouter(SecurePrefs securePrefs,
      DeepSeekProvider deepSeekProvider) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideProviderRouter(securePrefs, deepSeekProvider));
  }
}
