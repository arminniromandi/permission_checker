package ir.arminniromandi.permissionchecker.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ir.arminniromandi.permissionchecker.data.repository.AppScanRepositoryImpl
import ir.arminniromandi.permissionchecker.domain.repository.AppScanRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAppScanRepository(
        impl: AppScanRepositoryImpl
    ): AppScanRepository
}
