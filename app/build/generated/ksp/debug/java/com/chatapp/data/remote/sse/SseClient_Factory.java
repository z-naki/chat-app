package com.chatapp.data.remote.sse;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.serialization.json.Json;
import okhttp3.OkHttpClient;

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
public final class SseClient_Factory implements Factory<SseClient> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<Json> jsonProvider;

  public SseClient_Factory(Provider<OkHttpClient> okHttpClientProvider,
      Provider<Json> jsonProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
    this.jsonProvider = jsonProvider;
  }

  @Override
  public SseClient get() {
    return newInstance(okHttpClientProvider.get(), jsonProvider.get());
  }

  public static SseClient_Factory create(Provider<OkHttpClient> okHttpClientProvider,
      Provider<Json> jsonProvider) {
    return new SseClient_Factory(okHttpClientProvider, jsonProvider);
  }

  public static SseClient newInstance(OkHttpClient okHttpClient, Json json) {
    return new SseClient(okHttpClient, json);
  }
}
