package com.appcoins.wallet.billing.repository

import com.appcoins.wallet.billing.repository.entity.Sku
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

class RemoteRepository(private val api: BdsApi) {
  companion object {
    const val BASE_HOST = "https://api.blockchainds.com"
  }

  internal fun isBillingSupported(packageName: String,
                                  type: BillingSupportedType): Single<Boolean> {
    return api.getPackage(packageName, type.name.toLowerCase()).map { true }
  }

  fun getSkuDetails(packageName: String, skuIds: List<String>,
                    type: String): Single<List<Sku>> {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  interface BdsApi {
    @GET("inapp/8.20180518/packages/{packageName}")
    fun getPackage(@Path("packageName") packageName: String, @Query("type")
    type: String): Single<GetPackageResponse>
  }

  data class GetPackageResponse(val name: String)
}
