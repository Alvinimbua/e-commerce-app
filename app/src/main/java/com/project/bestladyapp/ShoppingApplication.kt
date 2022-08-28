package com.project.bestladyapp

import android.app.Application
import com.project.bestladyapp.data.source.repository.AuthRepoInterface
import com.project.bestladyapp.data.source.repository.ProductsRepoInterface

class ShoppingApplication : Application() {
	val authRepository: AuthRepoInterface
		get() = ServiceLocator.provideAuthRepository(this)

	val productsRepository: ProductsRepoInterface
		get() = ServiceLocator.provideProductsRepository(this)

	override fun onCreate() {
		super.onCreate()
	}
}