package com.chatapp.data.local.prefs;

import android.content.Context;
import com.chatapp.data.local.security.CryptoManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class SecurePrefs_Factory implements Factory<SecurePrefs> {
  private final Provider<Context> contextProvider;

  private final Provider<CryptoManager> cryptoManagerProvider;

  public SecurePrefs_Factory(Provider<Context> contextProvider,
      Provider<CryptoManager> cryptoManagerProvider) {
    this.contextProvider = contextProvider;
    this.cryptoManagerProvider = cryptoManagerProvider;
  }

  @Override
  public SecurePrefs get() {
    return newInstance(contextProvider.get(), cryptoManagerProvider.get());
  }

  public static SecurePrefs_Factory create(Provider<Context> contextProvider,
      Provider<CryptoManager> cryptoManagerProvider) {
    return new SecurePrefs_Factory(contextProvider, cryptoManagerProvider);
  }

  public static SecurePrefs newInstance(Context context, CryptoManager cryptoManager) {
    return new SecurePrefs(context, cryptoManager);
  }
}
