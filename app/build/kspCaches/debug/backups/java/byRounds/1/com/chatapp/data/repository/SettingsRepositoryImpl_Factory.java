package com.chatapp.data.repository;

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
public final class SettingsRepositoryImpl_Factory implements Factory<SettingsRepositoryImpl> {
  private final Provider<SecurePrefs> securePrefsProvider;

  public SettingsRepositoryImpl_Factory(Provider<SecurePrefs> securePrefsProvider) {
    this.securePrefsProvider = securePrefsProvider;
  }

  @Override
  public SettingsRepositoryImpl get() {
    return newInstance(securePrefsProvider.get());
  }

  public static SettingsRepositoryImpl_Factory create(Provider<SecurePrefs> securePrefsProvider) {
    return new SettingsRepositoryImpl_Factory(securePrefsProvider);
  }

  public static SettingsRepositoryImpl newInstance(SecurePrefs securePrefs) {
    return new SettingsRepositoryImpl(securePrefs);
  }
}
