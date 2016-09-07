package com.jifenke.lepluslive.weixin.service;

import com.jifenke.lepluslive.lejiauser.BarcodeConfig;
import com.jifenke.lepluslive.lejiauser.domain.entities.RegisterOrigin;
import com.jifenke.lepluslive.lejiauser.service.BarcodeService;
import com.jifenke.lepluslive.filemanage.service.FileImageService;
import com.jifenke.lepluslive.global.config.Constants;
import com.jifenke.lepluslive.global.util.MvUtil;
import com.jifenke.lepluslive.merchant.domain.entities.Merchant;
import com.jifenke.lepluslive.merchant.service.MerchantService;
import com.jifenke.lepluslive.partner.domain.entities.Partner;
import com.jifenke.lepluslive.partner.service.PartnerService;
import com.jifenke.lepluslive.score.domain.entities.ScoreA;
import com.jifenke.lepluslive.score.domain.entities.ScoreADetail;
import com.jifenke.lepluslive.score.domain.entities.ScoreB;
import com.jifenke.lepluslive.lejiauser.domain.entities.LeJiaUser;
import com.jifenke.lepluslive.score.domain.entities.ScoreBDetail;
import com.jifenke.lepluslive.score.repository.ScoreADetailRepository;
import com.jifenke.lepluslive.score.repository.ScoreBDetailRepository;
import com.jifenke.lepluslive.weixin.domain.entities.WeiXinUser;
import com.jifenke.lepluslive.score.repository.ScoreARepository;
import com.jifenke.lepluslive.score.repository.ScoreBRepository;
import com.jifenke.lepluslive.lejiauser.repository.LeJiaUserRepository;
import com.jifenke.lepluslive.weixin.repository.WeiXinUserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import javax.inject.Inject;

/**
 * Created by wcg on 16/3/18.
 */
@Service
@Transactional(readOnly = true)
public class WeiXinUserService {

  @Value("${bucket.ossBarCodeReadRoot}")
  private String barCodeRootUrl;

  @Inject
  private FileImageService fileImageService;

  @Inject
  private BarcodeService barcodeService;

  @Inject
  private WeiXinUserRepository weiXinUserRepository;

  @Inject
  private ScoreARepository scoreARepository;

  @Inject
  private ScoreBRepository scoreBRepository;

  @Inject
  private ScoreADetailRepository scoreADetailRepository;

  @Inject
  private LeJiaUserRepository leJiaUserRepository;

  @Inject
  private MerchantService merchantService;

  @Inject
  private ScoreBDetailRepository scoreBDetailRepository;

  @Inject
  private PartnerService partnerService;

  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public WeiXinUser findWeiXinUserByOpenId(String openId) {
    return weiXinUserRepository.findByOpenId(openId);
  }

  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public WeiXinUser findWeiXinUserByUnionId(String unionId) {
    return weiXinUserRepository.findByUnionId(unionId);
  }

  @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
  public WeiXinUser saveBarCodeForUser(WeiXinUser weiXinUser) throws IOException {
    LeJiaUser leJiaUser = weiXinUser.getLeJiaUser();
    byte[]
        bytes =
        barcodeService.barcode(leJiaUser.getUserSid(), BarcodeConfig.Barcode.defaultConfig());
    String filePath = MvUtil.getFilePath(Constants.BAR_CODE_EXT);
    fileImageService.SaveUserBarCode(bytes, filePath);

    leJiaUser.setOneBarCodeUrl(barCodeRootUrl + "/" + filePath);
    leJiaUserRepository.save(leJiaUser);
    return weiXinUser;
  }

  @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
  public void saveWeiXinUserByUnSubscribe(WeiXinUser weiXinUser) throws Exception {
    weiXinUserRepository.save(weiXinUser);
  }


  @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
  public void saveWeiXinUser(Map<String, Object> userDetail, Map<String, Object> map)
      throws IOException {
    String openid = userDetail.get("openid").toString();
    String
        unionId =
        userDetail.get("unionid") != null ? userDetail.get("unionid").toString() : null;
    WeiXinUser weiXinUser = weiXinUserRepository.findByOpenId(openid);
    ScoreA scoreA = null;
    ScoreB scoreB = null;
    if (weiXinUser == null) {
      weiXinUser = new WeiXinUser();
      weiXinUser.setLastUpdated(new Date());
      LeJiaUser leJiaUser = new LeJiaUser();
      leJiaUser.setHeadImageUrl(userDetail.get("headimgurl").toString());
      leJiaUser.setWeiXinUser(weiXinUser);
      RegisterOrigin registerOrigin = new RegisterOrigin();
      registerOrigin.setId(1L);
      leJiaUser.setRegisterOrigin(registerOrigin);
      leJiaUserRepository.save(leJiaUser);
      weiXinUser.setLeJiaUser(leJiaUser);
      scoreA = new ScoreA();
      scoreA.setScore(0L);
      scoreA.setTotalScore(0L);
      scoreA.setLeJiaUser(leJiaUser);
      scoreARepository.save(scoreA);
      scoreB = new ScoreB();
      scoreB.setScore(0L);
      scoreB.setTotalScore(0L);
      scoreB.setLeJiaUser(leJiaUser);
      scoreBRepository.save(scoreB);
    }

    weiXinUser.setOpenId(openid);
    weiXinUser.setUnionId(unionId);
    weiXinUser.setCity(userDetail.get("city").toString());
    weiXinUser.setCountry(userDetail.get("country").toString());
    weiXinUser.setSex(Long.parseLong(userDetail.get("sex").toString()));
    weiXinUser.setNickname(userDetail.get("nickname").toString());
    weiXinUser.setLanguage(userDetail.get("language").toString());
    weiXinUser.setHeadImageUrl(userDetail.get("headimgurl").toString());
    weiXinUser.setProvince(userDetail.get("province").toString());
    weiXinUser.setAccessToken(map.get("access_token").toString());
    weiXinUser.setRefreshToken(map.get("refresh_token").toString());
    weiXinUser.setLastUserInfoDate(new Date());
    weiXinUserRepository.save(weiXinUser);
  }

  @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
  public void saveWeiXinUserBySubscribe(Map<String, Object> userDetail, WeiXinUser weiXinUser)
      throws IOException {
    String openid = userDetail.get("openid").toString();
    String
        unionId =
        userDetail.get("unionid") != null ? userDetail.get("unionid").toString() : null;
    //  WeiXinUser weiXinUser = weiXinUserRepository.findByOpenId(openid);

    LeJiaUser leJiaUser = null;
    Date date = new Date();
    ScoreA scoreA = null;
    ScoreB scoreB = null;
    if (weiXinUser == null) {
      weiXinUser = new WeiXinUser();
      weiXinUser.setDateCreated(date);
      weiXinUser.setLastUpdated(date);
      leJiaUser = new LeJiaUser();
      leJiaUser.setHeadImageUrl(userDetail.get("headimgurl").toString());
      leJiaUser.setWeiXinUser(weiXinUser);
      RegisterOrigin registerOrigin = new RegisterOrigin();
      registerOrigin.setId(1L);
      leJiaUser.setRegisterOrigin(registerOrigin);
      leJiaUserRepository.save(leJiaUser);
      weiXinUser.setLeJiaUser(leJiaUser);
      scoreA = new ScoreA();
      scoreA.setScore(0L);
      scoreA.setTotalScore(0L);
      scoreA.setLeJiaUser(leJiaUser);
      scoreARepository.save(scoreA);
      scoreB = new ScoreB();
      scoreB.setScore(0L);
      scoreB.setTotalScore(0L);
      scoreB.setLeJiaUser(leJiaUser);
      scoreBRepository.save(scoreB);
    }

    leJiaUser = weiXinUser.getLeJiaUser();

    //判断是否需要绑定商户
    if (leJiaUser.getBindMerchant() == null) {
      Long merchantId = leJiaUserRepository.checkUserBindMerchant(leJiaUser.getId());
      if (merchantId != null) {
        Merchant merchant = merchantService.findMerchantById(merchantId);
        long userLimit = leJiaUserRepository.countMerchantBindLeJiaUser(merchantId);
        System.out.println(merchant.getUserLimit() < userLimit);
        if (merchant.getUserLimit() > userLimit) {
          leJiaUser.setBindMerchant(merchant);
          leJiaUser.setBindMerchantDate(date);
          Partner partner = merchant.getPartner();
          long partnerUserLimit = leJiaUserRepository.countPartnerBindLeJiaUser(partner.getId());
          if (partner.getUserLimit() > partnerUserLimit) {
            leJiaUser.setBindPartner(partner);
            leJiaUser.setBindPartnerDate(date);
          }
        }
      }
    }

    weiXinUser.setOpenId(openid);
    weiXinUser.setUnionId(unionId);
    weiXinUser.setCity(userDetail.get("city").toString());
    weiXinUser.setCountry(userDetail.get("country").toString());
    weiXinUser.setSex(Long.parseLong(userDetail.get("sex").toString()));
    weiXinUser.setNickname(userDetail.get("nickname").toString());
    weiXinUser.setLanguage(userDetail.get("language").toString());
    weiXinUser.setHeadImageUrl(userDetail.get("headimgurl").toString());
    weiXinUser.setProvince(userDetail.get("province").toString());
    weiXinUser.setLastUserInfoDate(date);
    // weiXinUser.setState(1);
    weiXinUser.setSubState(1);
    if (weiXinUser.getSubDate() == null) {
      weiXinUser.setSubDate(date);
    }
    if (weiXinUser.getSubSource() == null || "".equals(weiXinUser.getSubSource())) {
      weiXinUser.setSubSource(userDetail.get("subSource").toString());
    }
    weiXinUserRepository.save(weiXinUser);
  }

  /**
   * app的微信登录
   */
  @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
  public LeJiaUser saveWeiXinUserByApp(WeiXinUser weiXinUser, String unionId, String openid,
                                       String country, String city, String nickname,
                                       String province,
                                       String language, String headImgUrl, Long sex, String token)
      throws IOException {
    ScoreA scoreA = null;
    ScoreB scoreB = null;
    LeJiaUser leJiaUser = null;
    Date date = new Date();
    if (weiXinUser == null) {
      weiXinUser = new WeiXinUser();
      weiXinUser.setLastUpdated(date);
      leJiaUser = new LeJiaUser();
      leJiaUser.setHeadImageUrl(headImgUrl);
      leJiaUser.setToken(token);
      leJiaUser.setWeiXinUser(weiXinUser);
      RegisterOrigin registerOrigin = new RegisterOrigin();
      registerOrigin.setId(2L);
      leJiaUser.setRegisterOrigin(registerOrigin);
      leJiaUserRepository.save(leJiaUser);
      weiXinUser.setLeJiaUser(leJiaUser);
      scoreA = new ScoreA();
      scoreA.setScore(0L);
      scoreA.setTotalScore(0L);
      scoreA.setLeJiaUser(leJiaUser);
      scoreARepository.save(scoreA);
      scoreB = new ScoreB();
      scoreB.setScore(0L);
      scoreB.setTotalScore(0L);
      scoreB.setLeJiaUser(leJiaUser);
      scoreBRepository.save(scoreB);
    } else {
      leJiaUser = weiXinUser.getLeJiaUser();
    }

    weiXinUser.setAppOpenId(openid);
    weiXinUser.setUnionId(unionId);
    weiXinUser.setCity(city);
    weiXinUser.setCountry(country);
    weiXinUser.setSex(sex);
    weiXinUser.setNickname(nickname);
    weiXinUser.setLanguage(language);
    weiXinUser.setHeadImageUrl(headImgUrl);
    weiXinUser.setProvince(province);
    weiXinUser.setLastUserInfoDate(date);
    weiXinUser.setState(1);
    weiXinUserRepository.save(weiXinUser);

    return leJiaUser;
  }

//  @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
//  public void openHongBao(WeiXinUser weiXinUser, String phoneNumber) {
//    LeJiaUser leJiaUser = weiXinUser.getLeJiaUser();
//    leJiaUser.setPhoneNumber(phoneNumber);
//    leJiaUserRepository.save(leJiaUser);
//
////    ScoreA scoreA = scoreARepository.findByLeJiaUser(leJiaUser);
//    ScoreB scoreB = scoreBRepository.findByLeJiaUser(leJiaUser);
////    scoreA.setScore(scoreA.getScore() + 1000);
////    scoreA.setTotalScore(scoreA.getTotalScore() + 1000);
//    Date date = new Date();
////    scoreA.setLastUpdateDate(date);
//    scoreB.setLastUpdateDate(date);
//    scoreB.setScore(scoreB.getScore() + 100);
//    scoreB.setTotalScore(scoreB.getTotalScore() + 100);
//    scoreBRepository.save(scoreB);
////    scoreARepository.save(scoreA);
//
////    ScoreADetail scoreADetail = new ScoreADetail();
////    scoreADetail.setNumber(1000L);
////    scoreADetail.setScoreA(scoreA);
////    scoreADetail.setOperate("关注送红包");
////    scoreADetailRepository.save(scoreADetail);
//
//    ScoreBDetail scoreBDetail = new ScoreBDetail();
//    scoreBDetail.setNumber(100L);
//    scoreBDetail.setScoreB(scoreB);
//    scoreBDetail.setOperate("关注送积分");
//    scoreBDetailRepository.save(scoreBDetail);
//    weiXinUser.setHongBaoState(1);
//    weiXinUserRepository.save(weiXinUser);
//  }

  @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
  public void openHongBao(WeiXinUser weiXinUser, String phoneNumber) {
    LeJiaUser leJiaUser = weiXinUser.getLeJiaUser();
    leJiaUser.setPhoneNumber(phoneNumber);
    //  leJiaUser.setUserName(realName);
    leJiaUserRepository.save(leJiaUser);

    ScoreA scoreA = scoreARepository.findByLeJiaUser(leJiaUser).get(0);

    scoreA.setScore(scoreA.getScore() + 1000);
    scoreA.setTotalScore(scoreA.getTotalScore() + 1000);
    Date date = new Date();
    scoreA.setLastUpdateDate(date);

    scoreARepository.save(scoreA);

    ScoreADetail scoreADetail = new ScoreADetail();
    scoreADetail.setNumber(1000L);
    scoreADetail.setScoreA(scoreA);
    scoreADetail.setOperate("关注送红包");
    scoreADetailRepository.save(scoreADetail);

    weiXinUser.setHongBaoState(1);
    weiXinUser.setState(1);
    weiXinUserRepository.save(weiXinUser);
  }

  //普通关注送红包
  @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
  public int giveScoreAByDefault(WeiXinUser weiXinUser, int defaultScoreA, String phoneNumber) {
    LeJiaUser leJiaUser = weiXinUser.getLeJiaUser();
    ScoreA scoreA = scoreARepository.findByLeJiaUser(leJiaUser).get(0);
    ScoreB scoreB = scoreBRepository.findByLeJiaUser(leJiaUser);
    try {
      leJiaUser.setPhoneNumber(phoneNumber);
      leJiaUserRepository.save(leJiaUser);

      scoreA.setScore(scoreA.getScore() + defaultScoreA);
      scoreA.setTotalScore(scoreA.getTotalScore() + defaultScoreA);
      Date date = new Date();
      scoreA.setLastUpdateDate(date);
      scoreARepository.save(scoreA);

      scoreB.setLastUpdateDate(date);
      scoreB.setScore(scoreB.getScore() + 10);
      scoreB.setTotalScore(scoreB.getTotalScore() + 10);
      scoreBRepository.save(scoreB);

      ScoreADetail scoreADetail = new ScoreADetail();
      scoreADetail.setNumber(Long.valueOf(String.valueOf(defaultScoreA)));
      scoreADetail.setScoreA(scoreA);
      scoreADetail.setOperate("关注送红包");
      scoreADetail.setOrigin(0);
      scoreADetail.setOrderSid("0_" + defaultScoreA);
      scoreADetailRepository.save(scoreADetail);

      ScoreBDetail scoreBDetail = new ScoreBDetail();
      scoreBDetail.setNumber(10L);
      scoreBDetail.setScoreB(scoreB);
      scoreBDetail.setOperate("关注送积分");
      scoreBDetail.setOrigin(0);
      scoreBDetail.setOrderSid("0_10");
      scoreBDetailRepository.save(scoreBDetail);

      weiXinUser.setHongBaoState(1);
      weiXinUser.setState(1);
      weiXinUser.setStateDate(date);
      weiXinUserRepository.save(weiXinUser);
    } catch (Exception e) {
      e.printStackTrace();
      return 0;
    }
    return 1;
  }
}
