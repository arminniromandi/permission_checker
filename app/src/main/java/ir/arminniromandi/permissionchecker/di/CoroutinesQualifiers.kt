package ir.arminniromandi.permissionchecker.di

import javax.inject.Qualifier



/*
* علت اصلی ان جلوگیری از نشوتن hardcodded هستش
*
* */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher
