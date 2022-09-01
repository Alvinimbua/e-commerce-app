package com.project.bestladyapp.data.source.repository

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.project.bestladyapp.data.Result
import com.project.bestladyapp.data.Result.Error
import com.project.bestladyapp.data.Result.Success
import com.project.bestladyapp.data.ShoppingAppSessionManager
import com.project.bestladyapp.data.UserData
import com.project.bestladyapp.data.source.UserDataSource
import com.project.bestladyapp.data.utils.SignUpErrors
import com.project.bestladyapp.data.utils.UserType
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class AuthRepository(
	private val userLocalDataSource: UserDataSource,
	private val authRemoteDataSource: UserDataSource,
	private var sessionManager: ShoppingAppSessionManager
) : AuthRepoInterface {

	private var firebaseAuth: FirebaseAuth = Firebase.auth

	companion object {
		private const val TAG = "AuthRepository"
	}

	override fun getFirebaseAuth() = firebaseAuth

	override fun isRememberMeOn() = sessionManager.isRememberMeOn()

	override suspend fun refreshData() {
		Log.d(TAG, "refreshing userdata")
		if (sessionManager.isLoggedIn()) {
			updateUserInLocalSource(sessionManager.getPhoneNumber())
		} else {
			sessionManager.logoutFromSession()
			deleteUserFromLocalSource()
		}
	}

	override suspend fun signUp(userData: UserData,callback:()->Unit) {
		val isSeller = userData.userType == UserType.SELLER.name
		sessionManager.createLoginSession(
			userData.userId,
			userData.name,
			userData.mobile,
			false,
			isSeller
		)
		Log.d(TAG, "on SignUp: Updating user in Local Source")
		userLocalDataSource.addUser(userData, callback)
		Log.d(TAG, "on SignUp: Updating userdata on Remote Source")
		authRemoteDataSource.addUser(userData, callback)

	}

	override fun login(userData: UserData, rememberMe: Boolean) {
		val isSeller = userData.userType == UserType.SELLER.name
		sessionManager.createLoginSession(
			userData.userId,
			userData.name,
			userData.mobile,
			rememberMe,
			isSeller
		)
	}

	override suspend fun checkEmailAndMobile(
		 userData: UserData,
		context: Context,
		 callback: () -> Unit
	): SignUpErrors? {
		Log.d(TAG, "on SignUp: Checking email and mobile")
		var sErr: SignUpErrors? = null
		try {
			val queryResult = authRemoteDataSource.getEmailsAndMobiles()
			if (queryResult != null) {
				val mob = queryResult.mobiles.contains(userData.mobile)
				val em = queryResult.emails.contains(userData.email)
				if (!mob && !em) {
					sErr = SignUpErrors.NONE
				} else {
					sErr = SignUpErrors.SERR
					when {
						!mob && em -> makeErrToast("Email is already registered!", context)
						mob && !em -> makeErrToast("Mobile is already registered!", context)
						mob && em -> makeErrToast(
							"Email and mobile is already registered!",
							context
						)
					}
				}
			}else {
				addUser(userData, callback)
			}
		} catch (e: Exception) {
			makeErrToast("Some Error Occurred", context)
		}
		return sErr
	}

	private suspend fun addUser(userData: UserData,callback: () -> Unit){
		val isSeller = userData.userType == UserType.SELLER.name
		sessionManager.createLoginSession(
			userData.userId,
			userData.name,
			userData.mobile,
			false,
			isSeller
		)
		Log.d(TAG, "on SignUp: Updating user in Local Source")
		userLocalDataSource.addUser(userData,callback)
		Log.d(TAG, "on SignUp: Updating userdata on Remote Source")
		authRemoteDataSource.addUser(userData,callback)
	}

	override suspend fun checkLogin(mobile: String, password: String): UserData? {
		Log.d(TAG, "on Login: checking mobile and password")
		var queryResult = mutableListOf<UserData>()
		try {
			queryResult = authRemoteDataSource.getUserByMobileAndPassword(mobile, password)
		} catch (e: Exception) {
			// No Handling
		}
		return if (queryResult.size > 0) {
			queryResult[0]
		} else {
			null
		}

	}

	override fun signInWithPhoneAuthCredential(
		credential: PhoneAuthCredential,
		isUserLoggedIn: MutableLiveData<Boolean>, context: Context
	) {
		try {
			firebaseAuth.signInWithCredential(credential)
				.addOnCompleteListener { task ->
					if (task.isSuccessful) {
						Log.d(TAG, "signInWithCredential:success")
						val user = task.result?.user
						if (user != null) {
							isUserLoggedIn.postValue(true)
						}

					} else {
						Log.w(TAG, "signInWithCredential:failure", task.exception)
						if (task.exception is FirebaseAuthInvalidCredentialsException) {
							Log.d(TAG, "createUserWithMobile:failure", task.exception)
							isUserLoggedIn.postValue(false)
							makeErrToast("Wrong OTP!", context)
						}
					}
				}.addOnFailureListener {
					Log.d(TAG, "createUserWithMobile:failure", it)
					isUserLoggedIn.postValue(false)
					makeErrToast("Invalid Request!", context)
				}
		} catch (e: Exception) {
			makeErrToast("Some Error Occurred", context)
		}

	}


	override suspend fun signOut() {
		sessionManager.logoutFromSession()
		firebaseAuth.signOut()
		userLocalDataSource.clearUser()
	}

	private fun makeErrToast(text: String, context: Context) {
		Toast.makeText(context, text, Toast.LENGTH_LONG).show()
	}

	private suspend fun deleteUserFromLocalSource() {
		userLocalDataSource.clearUser()
	}

	private suspend fun updateUserInLocalSource(phoneNumber: String?) {
		coroutineScope {
			launch {
				if (phoneNumber != null) {
					val getUser = userLocalDataSource.getUserByMobile(phoneNumber)
					if (getUser == null) {
						userLocalDataSource.clearUser()
						val uData = authRemoteDataSource.getUserByMobile(phoneNumber)
						if (uData != null) {
							userLocalDataSource.addUser(uData)
						}
					}
				}
			}
		}
	}

	override suspend fun hardRefreshUserData() {
		userLocalDataSource.clearUser()
		val mobile = sessionManager.getPhoneNumber()
		if (mobile != null) {
			val uData = authRemoteDataSource.getUserByMobile(mobile)
			if (uData != null) {
				userLocalDataSource.addUser(uData)
			}
		}
	}

	override suspend fun insertProductToLikes(productId: String, userId: String): Result<Boolean> {
		return supervisorScope {
			val remoteRes = async {
				Log.d(TAG, "onLikeProduct: adding product to remote source")
				authRemoteDataSource.likeProduct(productId, userId)
			}
			val localRes = async {
				Log.d(TAG, "onLikeProduct: updating product to local source")
				userLocalDataSource.likeProduct(productId, userId)
			}
			try {
				localRes.await()
				remoteRes.await()
				Success(true)
			} catch (e: Exception) {
				Error(e)
			}
		}
	}

	override suspend fun removeProductFromLikes(
		productId: String,
		userId: String
	): Result<Boolean> {
		return supervisorScope {
			val remoteRes = async {
				Log.d(TAG, "onDislikeProduct: deleting product from remote source")
				authRemoteDataSource.dislikeProduct(productId, userId)
			}
			val localRes = async {
				Log.d(TAG, "onDislikeProduct: updating product to local source")
				userLocalDataSource.dislikeProduct(productId, userId)
			}
			try {
				localRes.await()
				remoteRes.await()
				Success(true)
			} catch (e: Exception) {
				Error(e)
			}
		}
	}

	override suspend fun insertAddress(
		newAddress: UserData.Address,
		userId: String
	): Result<Boolean> {
		return supervisorScope {
			val remoteRes = async {
				Log.d(TAG, "onInsertAddress: adding address to remote source")
				authRemoteDataSource.insertAddress(newAddress, userId)
			}
			val localRes = async {
				Log.d(TAG, "onInsertAddress: updating address to local source")
				val userRes = authRemoteDataSource.getUserById(userId)
				if (userRes is Success) {
					userLocalDataSource.clearUser()
					userLocalDataSource.addUser(userRes.data!!)
				} else if (userRes is Error) {
					throw userRes.exception
				}else{}
			}
			try {
				remoteRes.await()
				localRes.await()
				Success(true)
			} catch (e: Exception) {
				Error(e)
			}
		}
	}

	override suspend fun updateAddress(
		newAddress: UserData.Address,
		userId: String
	): Result<Boolean> {
		return supervisorScope {
			val remoteRes = async {
				Log.d(TAG, "onUpdateAddress: updating address on remote source")
				authRemoteDataSource.updateAddress(newAddress, userId)
			}
			val localRes = async {
				Log.d(TAG, "onUpdateAddress: updating address on local source")
				val userRes =
					authRemoteDataSource.getUserById(userId)
				if (userRes is Success) {
					userLocalDataSource.clearUser()
					userLocalDataSource.addUser(userRes.data!!)
				} else if (userRes is Error) {
					throw userRes.exception
				}
				else {

				}
			}
			try {
				remoteRes.await()
				localRes.await()
				Success(true)
			} catch (e: Exception) {
				Error(e)
			}
		}
	}

	override suspend fun deleteAddressById(addressId: String, userId: String): Result<Boolean> {
		return supervisorScope {
			val remoteRes = async {
				Log.d(TAG, "onDelete: deleting address from remote source")
				authRemoteDataSource.deleteAddress(addressId, userId)
			}
			val localRes = async {
				Log.d(TAG, "onDelete: deleting address from local source")
				val userRes =
					authRemoteDataSource.getUserById(userId)
				if (userRes is Success) {
					userLocalDataSource.clearUser()
					userLocalDataSource.addUser(userRes.data!!)
				} else if (userRes is Error) {
					throw userRes.exception
				} else {

				}
			}
			try {
				remoteRes.await()
				localRes.await()
				Success(true)
			} catch (e: Exception) {
				Error(e)
			}
		}
	}

	override suspend fun insertCartItemByUserId(
		cartItem: UserData.CartItem,
		userId: String
	): Result<Boolean> {
		return supervisorScope {
			val remoteRes = async {
				Log.d(TAG, "onInsertCartItem: adding item to remote source")
				authRemoteDataSource.insertCartItem(cartItem, userId)
			}
			val localRes = async {
				Log.d(TAG, "onInsertCartItem: updating item to local source")
				userLocalDataSource.insertCartItem(cartItem, userId)
			}
			try {
				localRes.await()
				remoteRes.await()
				Success(true)
			} catch (e: Exception) {
				Error(e)
			}
		}
	}

	override suspend fun updateCartItemByUserId(
		cartItem: UserData.CartItem,
		userId: String
	): Result<Boolean> {
		return supervisorScope {
			val remoteRes = async {
				Log.d(TAG, "onUpdateCartItem: updating cart item on remote source")
				authRemoteDataSource.updateCartItem(cartItem, userId)
			}
			val localRes = async {
				Log.d(TAG, "onUpdateCartItem: updating cart item on local source")
				userLocalDataSource.updateCartItem(cartItem, userId)
			}
			try {
				localRes.await()
				remoteRes.await()
				Success(true)
			} catch (e: Exception) {
				Error(e)
			}
		}
	}

	override suspend fun deleteCartItemByUserId(itemId: String, userId: String): Result<Boolean> {
		return supervisorScope {
			val remoteRes = async {
				Log.d(TAG, "onDelete: deleting cart item from remote source")
				authRemoteDataSource.deleteCartItem(itemId, userId)
			}
			val localRes = async {
				Log.d(TAG, "onDelete: deleting cart item from local source")
				userLocalDataSource.deleteCartItem(itemId, userId)
			}
			try {
				localRes.await()
				remoteRes.await()
				Success(true)
			} catch (e: Exception) {
				Error(e)
			}
		}
	}

	override suspend fun placeOrder(newOrder: UserData.OrderItem, userId: String): Result<Boolean> {
		return supervisorScope {
			val remoteRes = async {
				Log.d(TAG, "onPlaceOrder: adding item to remote source")
				authRemoteDataSource.placeOrder(newOrder, userId)
			}
			val localRes = async {
				Log.d(TAG, "onPlaceOrder: adding item to local source")
				val userRes = authRemoteDataSource.getUserById(userId)
				if (userRes is Success) {
					userLocalDataSource.clearUser()
					userLocalDataSource.addUser(userRes.data!!)
				} else if (userRes is Error) {
					throw userRes.exception
				} else {

				}
			}
			try {
				remoteRes.await()
				localRes.await()
				Success(true)
			} catch (e: Exception) {
				Error(e)
			}
		}
	}

	override suspend fun setStatusOfOrder(
		orderId: String,
		userId: String,
		status: String
	): Result<Boolean> {
		return supervisorScope {
			val remoteRes = async {
				Log.d(TAG, "onSetStatus: updating status on remote source")
				authRemoteDataSource.setStatusOfOrderByUserId(orderId, userId, status)
			}
			val localRes = async {
				Log.d(TAG, "onSetStatus: updating status on local source")
				userLocalDataSource.setStatusOfOrderByUserId(orderId, userId, status)
			}
			try {
				localRes.await()
				remoteRes.await()
				Success(true)
			} catch (e: Exception) {
				Error(e)
			}
		}
	}

	override suspend fun getOrdersByUserId(userId: String): Result<List<UserData.OrderItem>?> {
		return userLocalDataSource.getOrdersByUserId(userId)
	}

	override suspend fun getAddressesByUserId(userId: String): Result<List<UserData.Address>?> {
		return userLocalDataSource.getAddressesByUserId(userId)
	}

	override suspend fun getLikesByUserId(userId: String): Result<List<String>?> {
		return userLocalDataSource.getLikesByUserId(userId)
	}

	override suspend fun getUserData(userId: String): Result<UserData?> {
		return userLocalDataSource.getUserById(userId)
	}

}