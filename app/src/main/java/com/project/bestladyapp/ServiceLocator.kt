package com.project.bestladyapp

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.project.bestladyapp.data.ShoppingAppSessionManager
import com.project.bestladyapp.data.source.ProductDataSource
import com.project.bestladyapp.data.source.UserDataSource
import com.project.bestladyapp.data.source.local.ProductsLocalDataSource
import com.project.bestladyapp.data.source.local.ShoppingAppDatabase
import com.project.bestladyapp.data.source.local.UserLocalDataSource
import com.project.bestladyapp.data.source.remote.AuthRemoteDataSource
import com.project.bestladyapp.data.source.remote.ProductsRemoteDataSource
import com.project.bestladyapp.data.source.repository.AuthRepoInterface
import com.project.bestladyapp.data.source.repository.AuthRepository
import com.project.bestladyapp.data.source.repository.ProductsRepoInterface
import com.project.bestladyapp.data.source.repository.ProductsRepository

object ServiceLocator {
	private var database: ShoppingAppDatabase? = null
	private val lock = Any()

	@Volatile
	var authRepository: AuthRepoInterface? = null
		@VisibleForTesting set

	@Volatile
	var productsRepository: ProductsRepoInterface? = null
		@VisibleForTesting set

	fun provideAuthRepository(context: Context): AuthRepoInterface {
		synchronized(this) {
			return authRepository ?: createAuthRepository(context)
		}
	}

	fun provideProductsRepository(context: Context): ProductsRepoInterface {
		synchronized(this) {
			return productsRepository ?: createProductsRepository(context)
		}
	}

	@VisibleForTesting
	fun resetRepository() {
		synchronized(lock) {
			database?.apply {
				clearAllTables()
				close()
			}
			database = null
			authRepository = null
		}
	}

	private fun createProductsRepository(context: Context): ProductsRepoInterface {
		val newRepo =
			ProductsRepository(ProductsRemoteDataSource(), createProductsLocalDataSource(context))
		productsRepository = newRepo
		return newRepo
	}

	private fun createAuthRepository(context: Context): AuthRepoInterface {
		val appSession = ShoppingAppSessionManager(context.applicationContext)
		val newRepo =
			AuthRepository(createUserLocalDataSource(context), AuthRemoteDataSource(), appSession)
		authRepository = newRepo
		return newRepo
	}

	private fun createProductsLocalDataSource(context: Context): ProductDataSource {
		val database = database ?: ShoppingAppDatabase.getInstance(context.applicationContext)
		return ProductsLocalDataSource(database.productsDao())
	}

	private fun createUserLocalDataSource(context: Context): UserDataSource {
		val database = database ?: ShoppingAppDatabase.getInstance(context.applicationContext)
		return UserLocalDataSource(database.userDao())
	}
}