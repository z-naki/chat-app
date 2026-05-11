package com.chatapp.domain.usecase;

import com.chatapp.domain.repository.SettingsRepository;
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
public final class GetApiKeyUseCase_Factory implements Factory<GetApiKeyUseCase> {
  private final Provider<SettingsRepository> settingsRepositoryProvider;

  public GetApiKeyUseCase_Factory(Provider<SettingsRepository> settingsRepositoryProvider) {
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public GetApiKeyUseCase get() {
    return newInstance(settingsRepositoryProvider.get());
  }

  public static GetApiKeyUseCase_Factory create(
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new GetApiKeyUseCase_Factory(settingsRepositoryProvider);
  }

  public static GetApiKeyUseCase newInstance(SettingsRepository settingsRepository) {
    return new GetApiKeyUseCase(settingsRepository);
  }
}
