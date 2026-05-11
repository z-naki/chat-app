package com.chatapp.data.remote.provider;

import com.chatapp.data.local.prefs.SecurePrefs;
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
public final class ProviderRouter_Factory implements Factory<ProviderRouter> {
  private final Provider<SecurePrefs> securePrefsProvider;

  public ProviderRouter_Factory(Provider<SecurePrefs> securePrefsProvider) {
    this.securePrefsProvider = securePrefsProvider;
  }

  @Override
  public ProviderRouter get() {
    return newInstance(securePrefsProvider.get());
  }

  public static ProviderRouter_Factory create(Provider<SecurePrefs> securePrefsProvider) {
    return new ProviderRouter_Factory(securePrefsProvider);
  }

  public static ProviderRouter newInstance(SecurePrefs securePrefs) {
    return new ProviderRouter(securePrefs);
  }
}
