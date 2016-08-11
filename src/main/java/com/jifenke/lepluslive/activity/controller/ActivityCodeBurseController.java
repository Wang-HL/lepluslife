package com.jifenke.lepluslive.activity.controller;

import com.jifenke.lepluslive.activity.domain.entities.ActivityCodeBurse;
import com.jifenke.lepluslive.activity.domain.entities.ActivityJoinLog;
import com.jifenke.lepluslive.activity.service.ActivityCodeBurseService;
import com.jifenke.lepluslive.activity.service.ActivityJoinLogService;
import com.jifenke.lepluslive.global.util.CookieUtils;
import com.jifenke.lepluslive.global.util.LejiaResult;
import com.jifenke.lepluslive.global.util.MvUtil;
import com.jifenke.lepluslive.score.service.ScoreAService;
import com.jifenke.lepluslive.weixin.domain.entities.WeiXinUser;
import com.jifenke.lepluslive.weixin.service.DictionaryService;
import com.jifenke.lepluslive.weixin.service.WeiXinUserService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.Date;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

/**
 * Created by wcg on 16/3/17.
 */
@RestController
@RequestMapping("/weixin")
public class ActivityCodeBurseController {

  @Value("${weixin.appId}")
  private String appId;
  @Inject
  private ActivityCodeBurseService activityCodeBurseService;
  @Inject
  private WeiXinUserService weiXinUserService;
  @Inject
  private ActivityJoinLogService activityJoinLogService;

  @Inject
  private ScoreAService scoreAService;

  @Inject
  private DictionaryService dictionaryService;

  @RequestMapping(value = "/activity/{id}", method = RequestMethod.GET)
  public ModelAndView goActivityPage(HttpServletRequest request, @PathVariable String id,
                                     Model model) {
    String openId = CookieUtils.getCookieValue(request, appId + "-user-open-id");
    WeiXinUser weiXinUser = weiXinUserService.findWeiXinUserByOpenId(openId);
    String[] str = id.split("_");
    if ("0".equals(str[0])) { //普通关注
      //判断是否获得过红包
      ActivityJoinLog joinLog = activityJoinLogService.findLogBySubActivityAndOpenId(0, weiXinUser
          .getOpenId());
      int defaultScoreA = Integer.valueOf(dictionaryService.findDictionaryById(18L).getValue());
      if (joinLog == null) {//未参与
        //派发红包,获取默认派发红包金额
        int status = scoreAService.giveScoreAByDefault(weiXinUser, defaultScoreA);
        //添加参加记录
        if (status == 1) {
          activityJoinLogService.addCodeBurseLogByDefault(weiXinUser, defaultScoreA);
        }
      }
      model.addAttribute("singleMoney", defaultScoreA);
      model.addAttribute("status", 200);
    } else {
      //判断活动是否失效
      ActivityCodeBurse
          codeBurse =
          activityCodeBurseService.findCodeBurseById(Long.valueOf(str[1]));
      if (codeBurse != null) {
        //活动优先级最高(已结束||暂停||派发完毕)
        if (codeBurse.getEndDate().getTime() < new Date().getTime() || codeBurse.getState() == 0
            || codeBurse.getBudget().intValue() < codeBurse.getTotalMoney().intValue()) {
          model.addAttribute("status", 201);
          model.addAttribute("singleMoney", codeBurse.getSingleMoney());
        } else {
          //判断是否参与过该种活动
          ActivityJoinLog
              joinLog =
              activityJoinLogService
                  .findLogBySubActivityAndOpenId(codeBurse.getType(), weiXinUser.getOpenId());
          if (joinLog == null) {//未参与
            int status = scoreAService.giveScoreAByActivity(codeBurse, weiXinUser.getLeJiaUser());
            //添加参加记录
            if (status == 1) {
              activityJoinLogService.addCodeBurseLog(codeBurse, weiXinUser);
            }
            model.addAttribute("singleMoney", codeBurse.getSingleMoney());
          } else {
            model.addAttribute("singleMoney", joinLog.getDetail());
          }
          model.addAttribute("status", 200);
        }
      } else {
        model.addAttribute("status", 404);
      }
    }
    // model.addAttribute("wxConfig", weiXinService.getWeiXinConfig(request));
    return MvUtil.go("/activity/codeBurse");
  }

  @RequestMapping(value = "/codeBurse/ajaxList", method = RequestMethod.POST)
  public
  @ResponseBody
  LejiaResult getCodeBurses(@RequestParam Integer offset) {
    if (offset == null) {
      offset = 1;
    }
    Page page = activityCodeBurseService.findByPage(offset, 10);

    return LejiaResult.ok(page);
  }

}
