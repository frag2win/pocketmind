package com.frag2win.pocketmind.di

import com.frag2win.pocketmind.data.inference.InferenceFactory
import com.frag2win.pocketmind.domain.inference.PocketMindInference
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing inference-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object InferenceModule {

    @Provides
    @Singleton
    fun providePocketMindInference(factory: InferenceFactory): PocketMindInference {
        return factory.getInferenceImplementation()
    }
}
