package com.asfoundation.wallet.subscriptions

import io.reactivex.Single

interface SubscriptionApiMocked {

  fun getActiveSubscriptions(walletAddress: String): Single<List<Subscription>>
  fun getExpiredSubscriptions(walletAddress: String): Single<List<Subscription>>
  fun getSubscriptionDetails(packageName: String, address: String): Single<Subscription>
  fun getSubscriptionByTransactionId(transactionId: String): Single<Subscription>

}