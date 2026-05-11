package com.chatapp.data.remote.provider.deepseek;

import com.chatapp.data.local.prefs.SecurePrefs;
import com.chatapp.data.remote.sse.SseClient;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.serialization.json.Json;

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
public final class DeepSeekProvider_Factory implements Factory<DeepSeekProvider> {
  private final Provider<SseClient> sseClientProvider;

  private final Provider<SecurePrefs> securePrefsProvider;

  private final Provider<Json> jsonProvider;

  public DeepSeekProvider_Factory(Provider<SseClient> sseClientProvider,
      Provider<SecurePrefs> securePrefsProvider, Provider<Json> jsonProvider) {
    this.sseClientProvider = sseClientProvider;
    this.securePrefsProvider = securePrefsProvider;
    this.jsonProvider = jsonProvider;
  }

  @Override
  public DeepSeekProvider get() {
    return newInstance(sseClientProvider.get(), securePrefsProvider.get(), jsonProvider.get());
  }

  public static DeepSeekProvider_Factory create(Provider<SseClient> sseClientProvider,
      Provider<SecurePrefs> securePrefsProvider, Provider<Json> jsonProvider) {
    return new DeepSeekProvider_Factory(sseClientProvider, securePrefsProvider, jsonProvider);
  }

  public static DeepSeekProvider newInstance(SseClient sseClient, SecurePrefs securePrefs,
      Json json) {
    return new DeepSeekProvider(sseClient, securePrefs, json);
  }
}
