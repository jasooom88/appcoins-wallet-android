package com.asfoundation.wallet.ui.iab.payments.carrier.verify

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.asf.wallet.R
import com.jakewharton.rxbinding2.view.RxView
import dagger.android.support.DaggerFragment
import io.reactivex.Observable
import kotlinx.android.synthetic.main.dialog_buy_buttons_payment_methods.*
import kotlinx.android.synthetic.main.fragment_carrier_verify_phone.*
import java.math.BigDecimal
import java.util.*
import javax.inject.Inject

class CarrierVerifyFragment : DaggerFragment(),
    CarrierVerifyView {

  @Inject
  lateinit var presenter: CarrierVerifyPresenter

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_carrier_verify_phone, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    setupUi()
    super.onViewCreated(view, savedInstanceState)
    presenter.present()

  }

  override fun onDestroyView() {
    presenter.stop()
    super.onDestroyView()
  }

  private fun setupUi() {
    cancel_button.setText(R.string.back_button)
    cancel_button.visibility = View.VISIBLE

    buy_button.setText(R.string.action_next)
    buy_button.visibility = View.VISIBLE
    buy_button.isEnabled = false
  }


  override fun initializeView(appName: String, icon: Drawable,
                              currency: String,
                              fiatAmount: BigDecimal, appcAmount: BigDecimal,
                              skuDescription: String,
                              bonusAmount: BigDecimal) {
    buy_button.isEnabled = true
    payment_methods_header.setTitle(appName)
    payment_methods_header.setIcon(icon)
    payment_methods_header.setDescription(skuDescription)
    payment_methods_header.setPrice(fiatAmount, appcAmount, currency)
    payment_methods_header.showPrice()
    payment_methods_header.hideSkeleton()
    purchase_bonus.setPurchaseBonusHeaderValue(bonusAmount, mapCurrencyCodeToSymbol(currency))
    purchase_bonus.hideSkeleton()
  }

  private fun mapCurrencyCodeToSymbol(currencyCode: String): String {
    return if (currencyCode.equals("APPC", ignoreCase = true))
      currencyCode
    else
      Currency.getInstance(currencyCode)
          .symbol
  }

  override fun backEvent(): Observable<Any> {
    return RxView.clicks(cancel_button)
  }

  override fun nextClickEvent(): Observable<Any> {
    return RxView.clicks(buy_button)
  }

  companion object {

    internal const val TRANSACTION_TYPE_KEY = "type"
    internal const val DOMAIN_KEY = "domain"
    internal const val TRANSACTION_DATA_KEY = "transaction_data"
    internal const val APPC_AMOUNT_KEY = "appc_amount"
    internal const val FIAT_AMOUNT_KEY = "fiat_amount"
    internal const val CURRENCY_KEY = "currency"
    internal const val BONUS_AMOUNT_KEY = "bonus_amount"
    internal const val SKU_DESCRIPTION = "sku_description"

    @JvmStatic
    fun newInstance(domain: String, transactionType: String, transactionData: String?,
                    currency: String?, amount: BigDecimal, appcAmount: BigDecimal,
                    bonus: BigDecimal?, skuDescription: String): CarrierVerifyFragment {
      val fragment =
          CarrierVerifyFragment()


      fragment.arguments = Bundle().apply {
        putString(
            DOMAIN_KEY, domain)
        putString(
            TRANSACTION_TYPE_KEY, transactionType)
        putString(
            TRANSACTION_DATA_KEY, transactionData)
        putString(
            CURRENCY_KEY, currency)
        putSerializable(
            FIAT_AMOUNT_KEY, amount)
        putSerializable(
            APPC_AMOUNT_KEY, appcAmount)
        putSerializable(
            BONUS_AMOUNT_KEY, bonus)
        putString(
            SKU_DESCRIPTION, skuDescription)
      }
      return fragment
    }
  }
}