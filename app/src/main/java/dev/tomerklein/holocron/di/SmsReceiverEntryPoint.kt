package dev.tomerklein.holocron.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.tomerklein.holocron.data.HolocronRepository
import dev.tomerklein.holocron.dispatch.DispatchEnqueuer
import dev.tomerklein.holocron.rules.NumberMatcher

/**
 * Lets the manifest-registered [dev.tomerklein.holocron.sms.SmsReceiver] (which Hilt can't
 * field-inject) pull the singletons it needs via [dagger.hilt.android.EntryPointAccessors].
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SmsReceiverEntryPoint {
    fun repository(): HolocronRepository
    fun matcher(): NumberMatcher
    fun enqueuer(): DispatchEnqueuer
}
