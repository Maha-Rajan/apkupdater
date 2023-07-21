package com.apkupdater.di

import android.content.Context
import androidx.work.WorkManager
import com.apkupdater.BuildConfig
import com.google.gson.GsonBuilder
import com.kryptoprefs.preferences.KryptoBuilder
import com.apkupdater.R
import com.apkupdater.prefs.Prefs
import com.apkupdater.repository.ApkMirrorRepository
import com.apkupdater.repository.AppsRepository
import com.apkupdater.repository.GitHubRepository
import com.apkupdater.repository.UpdatesRepository
import com.apkupdater.service.ApkMirrorService
import com.apkupdater.service.GitHubService
import com.apkupdater.util.NotificationUtil
import com.apkupdater.viewmodel.AppsViewModel
import com.apkupdater.viewmodel.MainViewModel
import com.apkupdater.viewmodel.SearchViewModel
import com.apkupdater.viewmodel.SettingsViewModel
import com.apkupdater.viewmodel.UpdatesViewModel
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


val mainModule = module {

	single { GsonBuilder().create() }

	single { Cache(androidContext().cacheDir, 5 * 1024 * 1024) }

	single {
		HttpLoggingInterceptor().apply {
			level = HttpLoggingInterceptor.Level.BODY
		}
	}

	single { OkHttpClient
		.Builder()
		.cache(get())
		.addNetworkInterceptor { chain ->
			chain.proceed(
				chain.request()
					.newBuilder()
					.header("User-Agent", "APKUpdater-v" + BuildConfig.VERSION_NAME)
					.build()
			)
		}
		.addInterceptor(get<HttpLoggingInterceptor>())
		.build()
	}

	single(named("apkmirror")) {
		Retrofit.Builder()
			.client(get())
			.baseUrl("https://www.apkmirror.com")
			.addConverterFactory(GsonConverterFactory.create(get()))
			.build()
	}

	single(named("github")) {
		Retrofit.Builder()
			.client(get())
			.baseUrl("https://api.github.com")
			.addConverterFactory(GsonConverterFactory.create(get()))
			.build()
	}

	single { get<Retrofit>(named("apkmirror")).create(ApkMirrorService::class.java) }

	single { get<Retrofit>(named("github")).create(GitHubService::class.java) }

	single { ApkMirrorRepository(get(), get(), get<Context>().packageManager) }

	single { AppsRepository(get(), get()) }

	single { GitHubRepository(get()) }

	single { UpdatesRepository(get(), get(), get(), get()) }

	single { KryptoBuilder.hybrid(get(), androidContext().getString(R.string.app_name)) }

	single { Prefs(get()) }

	single { NotificationUtil(get()) }

	viewModel { parameters -> AppsViewModel(parameters.get(), get(), get()) }

	viewModel { MainViewModel() }

	viewModel { parameters -> UpdatesViewModel(parameters.get(), get()) }

	viewModel { SettingsViewModel(get(), get(), WorkManager.getInstance(get())) }

	viewModel { parameters -> SearchViewModel(parameters.get(), get()) }

}
