package com.asfoundation.wallet.promotions

import com.appcoins.wallet.gamification.GamificationScreen
import com.appcoins.wallet.gamification.repository.Levels
import com.appcoins.wallet.gamification.repository.PromotionsRepository
import com.appcoins.wallet.gamification.repository.entity.GamificationResponse
import com.appcoins.wallet.gamification.repository.entity.GenericResponse
import com.appcoins.wallet.gamification.repository.entity.ReferralResponse
import com.appcoins.wallet.gamification.repository.entity.UserStatusResponse
import com.asf.wallet.R
import com.asfoundation.wallet.interact.FindDefaultWalletInteract
import com.asfoundation.wallet.referrals.ReferralInteractorContract
import com.asfoundation.wallet.referrals.ReferralsScreen
import com.asfoundation.wallet.ui.gamification.GamificationInteractor
import com.asfoundation.wallet.ui.gamification.GamificationMapper
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import java.util.concurrent.TimeUnit

class PromotionsInteractor(private val referralInteractor: ReferralInteractorContract,
                           private val gamificationInteractor: GamificationInteractor,
                           private val promotionsRepo: PromotionsRepository,
                           private val findWalletInteract: FindDefaultWalletInteract,
                           private val mapper: GamificationMapper) :
    PromotionsInteractorContract {

  companion object {
    const val GAMIFICATION_ID = "GAMIFICATION"
    const val REFERRAL_ID = "REFERRAL"
    const val DEFAULT_VIEW_TYPE = "DEFAULT"
    const val PROGRESS_VIEW_TYPE = "PROGRESS"
  }

  override fun retrievePromotions(): Single<PromotionsModel> {
    return findWalletInteract.find()
        .flatMap {
          Single.zip(
              gamificationInteractor.getLevels(),
              promotionsRepo.getUserStatus(it.address),
              BiFunction { level: Levels, userStatsResponse: UserStatusResponse ->
                mapToPromotionsModel(userStatsResponse, level)
              }
          )
        }
  }

  override fun hasAnyPromotionUpdate(referralsScreen: ReferralsScreen,
                                     gamificationScreen: GamificationScreen): Single<Boolean> {
    return Single.just(false)
  }

  //Referrals
  override fun hasReferralUpdate(friendsInvited: Int, isVerified: Boolean,
                                 screen: ReferralsScreen): Single<Boolean> {
    return findWalletInteract.find()
        .flatMap {
          referralInteractor.hasReferralUpdate(it.address, friendsInvited, isVerified, screen)
        }
  }

  override fun saveReferralInformation(friendsInvited: Int, isVerified: Boolean,
                                       screen: ReferralsScreen): Completable {
    return referralInteractor.saveReferralInformation(friendsInvited, isVerified, screen)
  }

  override fun hasGamificationNewLevel(screen: GamificationScreen): Single<Boolean> {
    return gamificationInteractor.hasNewLevel(screen)
  }

  override fun levelShown(level: Int, promotions: GamificationScreen): Completable {
    return gamificationInteractor.levelShown(level, promotions)
  }

  private fun mapToPromotionsModel(userStatus: UserStatusResponse,
                                   levels: Levels): PromotionsModel {
    var gamificationAvailable = false
    var referralsAvailable = false
    var perksAvailable = false
    val promotions = mutableListOf<Promotion>()
    var maxBonus = 0.0
    userStatus.promotions.sortedByDescending { it.priority }
        .forEach {
          when (it) {
            is GamificationResponse -> {
              gamificationAvailable = it.status == GamificationResponse.Status.ACTIVE
              promotions.add(mapToGamificationItem(it))

              if (levels.isActive) {
                maxBonus = levels.list.maxBy { level -> level.bonus }?.bonus ?: 0.0
              }

              if (gamificationAvailable) {
                promotions.add(0,
                    TitleItem(R.string.perks_gamif_title, R.string.perks_gamif_subtitle, true,
                        maxBonus.toString()))
              }
            }
            is ReferralResponse -> {
              referralsAvailable = it.status == ReferralResponse.Status.ACTIVE
              promotions.add(mapToReferralItem(it))
            }
            is GenericResponse -> {
              perksAvailable = true

              when {
                isFuturePromotion(it) -> mapToFutureItem(it)
                it.viewType == DEFAULT_VIEW_TYPE -> promotions.add(mapToDefaultItem(it))
                else -> promotions.add(mapToProgressItem(it))
              }

              if (isValidGamificationLink(it.linkedPromotionId, gamificationAvailable,
                      it.startDate ?: 0)) {
                mapToGamificationLinkItem(promotions, it)
              }
            }
          }
        }

    if (perksAvailable) {
      promotions.add(2,
          TitleItem(R.string.perks_title, R.string.perks_body, false))
    }

    return PromotionsModel(gamificationAvailable, referralsAvailable, promotions, maxBonus)
  }

  private fun mapToGamificationLinkItem(promotions: MutableList<Promotion>,
                                        genericResponse: GenericResponse) {
    val gamificationItem = promotions[1] as GamificationItem
    gamificationItem.links.add(GamificationLinkItem(genericResponse.title, genericResponse.icon))
  }

  private fun mapToProgressItem(genericResponse: GenericResponse): ProgressItem {
    return ProgressItem(genericResponse.id, genericResponse.title, genericResponse.icon,
        genericResponse.endDate, genericResponse.currentProgress!!,
        genericResponse.objectiveProgress!!)
  }

  private fun mapToDefaultItem(genericResponse: GenericResponse): DefaultItem {
    return DefaultItem(genericResponse.id, genericResponse.title, genericResponse.icon,
        genericResponse.endDate)
  }

  private fun mapToGamificationItem(gamificationResponse: GamificationResponse): GamificationItem {
    val currentLevelInfo = mapper.mapCurrentLevelInfo(gamificationResponse.level)

    return GamificationItem(gamificationResponse.id, currentLevelInfo.planet,
        currentLevelInfo.levelColor, currentLevelInfo.title, currentLevelInfo.phrase,
        gamificationResponse.bonus, mutableListOf())
  }

  private fun mapToReferralItem(referralResponse: ReferralResponse): ReferralItem {
    return ReferralItem(referralResponse.id, referralResponse.amount, referralResponse.currency,
        referralResponse.link.orEmpty())
  }

  private fun mapToFutureItem(genericResponse: GenericResponse): FutureItem {
    return FutureItem(genericResponse.id, genericResponse.title, genericResponse.icon)
  }

  private fun isValidGamificationLink(linkedPromotionId: String?,
                                      gamificationAvailable: Boolean, startDate: Long): Boolean {
    val currentTime = TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
    return linkedPromotionId != null && linkedPromotionId == GAMIFICATION_ID && gamificationAvailable && startDate < currentTime
  }

  private fun isFuturePromotion(genericResponse: GenericResponse): Boolean {
    val currentTime = TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
    return genericResponse.startDate ?: 0 > currentTime
  }

}