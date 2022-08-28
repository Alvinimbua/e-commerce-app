package com.project.bestladyapp.data.source.local

import android.util.Log
import com.project.bestladyapp.data.Result
import com.project.bestladyapp.data.Result.*
import com.project.bestladyapp.data.UserData
import com.project.bestladyapp.data.source.UserDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserLocalDataSource internal constructor(
	private val userDao: UserDao,
	private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : UserDataSource {

	override suspend fun addUser(userData: UserData) {
		withContext(ioDispatcher) {
			userDao.clear()
			userDao.insert(userData)
		}
	}

	override suspend fun getUserById(userId: String): Result<UserData?> =
		withContext(ioDispatcher) {
			try {
				val uData = userDao.getById(userId)
				if (uData != null) {
					return@withContext Success(uData)
				} else {
					return@withContext Error(Exception("User Not Found!"))
				}
			} catch (e: Exception) {
				return@withContext Error(e)
			}
		}

	override suspend fun getUserByMobile(phoneNumber: String): UserData? =
		withContext(ioDispatcher) {
			try {
				val uData = userDao.getByMobile(phoneNumber)
				if (uData != null) {
					return@withContext uData
				} else {
					return@withContext null
				}
			} catch (e: Exception) {
				Log.d("UserLocalSource", "onGetUser: Error Occurred, $e")
				return@withContext null
			}
		}

	override suspend fun getOrdersByUserId(userId: String): Result<List<UserData.OrderItem>?> =
		withContext(ioDispatcher) {
			try {
				val uData = userDao.getById(userId)
				if (uData != null) {
					val ordersList = uData.orders
					return@withContext Success(ordersList)
				} else {
					return@withContext Error(Exception("User Not Found"))
				}

			} catch (e: Exception) {
				Log.d("UserLocalSource", "onGetOrders: Error Occurred, ${e.message}")
				return@withContext Error(e)
			}
		}

	override suspend fun getAddressesByUserId(userId: String): Result<List<UserData.Address>?> =
		withContext(ioDispatcher) {
			try {
				val uData = userDao.getById(userId)
				if (uData != null) {
					val addressList = uData.addresses
					return@withContext Success(addressList)
				} else {
					return@withContext Error(Exception("User Not Found"))
				}

			} catch (e: Exception) {
				Log.d("UserLocalSource", "onGetAddress: Error Occurred, ${e.message}")
				return@withContext Error(e)
			}
		}

	override suspend fun getLikesByUserId(userId: String): Result<List<String>?> =
		withContext(ioDispatcher) {
			try {
				val uData = userDao.getById(userId)
				if (uData != null) {
					val likesList = uData.likes
					return@withContext Success(likesList)
				} else {
					return@withContext Error(Exception("User Not Found"))
				}

			} catch (e: Exception) {
				Log.d("UserLocalSource", "onGetLikes: Error Occurred, ${e.message}")
				return@withContext Error(e)
			}
		}

	override suspend fun dislikeProduct(productId: String, userId: String) =
		withContext(ioDispatcher) {
			try {
				val uData = userDao.getById(userId)
				if (uData != null) {
					val likesList = uData.likes.toMutableList()
					likesList.remove(productId)
					uData.likes = likesList
					userDao.updateUser(uData)
				} else {
					throw Exception("User Not Found")
				}
			} catch (e: Exception) {
				Log.d("UserLocalSource", "onGetLikes: Error Occurred, ${e.message}")
				throw e
			}
		}

	override suspend fun likeProduct(productId: String, userId: String) =
		withContext(ioDispatcher) {
			try {
				val uData = userDao.getById(userId)
				if (uData != null) {
					val likesList = uData.likes.toMutableList()
					likesList.add(productId)
					uData.likes = likesList
					userDao.updateUser(uData)
				} else {
					throw Exception("User Not Found")
				}
			} catch (e: Exception) {
				Log.d("UserLocalSource", "onGetLikes: Error Occurred, ${e.message}")
				throw e
			}
		}

	override suspend fun insertCartItem(newItem: UserData.CartItem, userId: String) =
		withContext(ioDispatcher) {
			try {
				val uData = userDao.getById(userId)
				if (uData != null) {
					val cartItems = uData.cart.toMutableList()
					cartItems.add(newItem)
					uData.cart = cartItems
					userDao.updateUser(uData)
				} else {
					throw Exception("User Not Found")
				}
			} catch (e: Exception) {
				Log.d("UserLocalSource", "onInsertCartItem: Error Occurred, ${e.message}")
				throw e
			}
		}

	override suspend fun updateCartItem(item: UserData.CartItem, userId: String) =
		withContext(ioDispatcher) {
			try {
				val uData = userDao.getById(userId)
				if (uData != null) {
					val cartItems = uData.cart.toMutableList()
					val pos = cartItems.indexOfFirst { it.itemId == item.itemId }
					if (pos >= 0) {
						cartItems[pos] = item
					}
					uData.cart = cartItems
					userDao.updateUser(uData)
				} else {
					throw Exception("User Not Found")
				}
			} catch (e: Exception) {
				Log.d("UserLocalSource", "onInsertCartItem: Error Occurred, ${e.message}")
				throw e
			}
		}

	override suspend fun deleteCartItem(itemId: String, userId: String) =
		withContext(ioDispatcher) {
			try {
				val uData = userDao.getById(userId)
				if (uData != null) {
					val cartItems = uData.cart.toMutableList()
					val pos = cartItems.indexOfFirst { it.itemId == itemId }
					if (pos >= 0) {
						cartItems.removeAt(pos)
					}
					uData.cart = cartItems
					userDao.updateUser(uData)
				} else {
					throw Exception("User Not Found")
				}
			} catch (e: Exception) {
				Log.d("UserLocalSource", "onInsertCartItem: Error Occurred, ${e.message}")
				throw e
			}
		}

	override suspend fun setStatusOfOrderByUserId(orderId: String, userId: String, status: String) =
		withContext(ioDispatcher) {
			try {
				val uData = userDao.getById(userId)
				if (uData != null) {
					val orders = uData.orders.toMutableList()
					val pos = orders.indexOfFirst { it.orderId == orderId }
					if (pos >= 0) {
						orders[pos].status = status
						val custId = orders[pos].customerId
						val custData = userDao.getById(custId)
						if (custData != null) {
							val orderList = custData.orders.toMutableList()
							val idx = orderList.indexOfFirst { it.orderId == orderId }
							if (idx >= 0) {
								orderList[idx].status = status
							}
							custData.orders = orderList
							userDao.updateUser(custData)
						}
					}
					uData.orders = orders
					userDao.updateUser(uData)
				} else {
					throw Exception("User Not Found")
				}
			} catch (e: Exception) {
				Log.d("UserLocalSource", "onInsertCartItem: Error Occurred, ${e.message}")
				throw e
			}
		}

	override suspend fun clearUser() {
		withContext(ioDispatcher) {
			userDao.clear()
		}
	}

}