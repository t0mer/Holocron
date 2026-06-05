package dev.tomerklein.holocron.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.tomerklein.holocron.data.SettingsStore
import dev.tomerklein.holocron.ingest.IncomingMessageRouter

/**
 * Lets the manifest-registered receivers (which Hilt can't field-inject) pull the singletons
 * they need via [dagger.hilt.android.EntryPointAccessors].
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SmsReceiverEntryPoint {
    fun router(): IncomingMessageRouter
    fun settingsStore(): SettingsStore
}
