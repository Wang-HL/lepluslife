package com.jifenke.lepluslive.merchant.controller;

import com.jifenke.lepluslive.global.util.LejiaResult;
import com.jifenke.lepluslive.global.util.MvUtil;
import com.jifenke.lepluslive.global.util.PaginationUtil;
import com.jifenke.lepluslive.merchant.controller.dto.MerchantDto;
import com.jifenke.lepluslive.merchant.domain.entities.Merchant;
import com.jifenke.lepluslive.merchant.domain.entities.MerchantDetail;
import com.jifenke.lepluslive.merchant.domain.entities.MerchantScroll;
import com.jifenke.lepluslive.merchant.service.MerchantService;
import com.jifenke.lepluslive.weixin.service.WeiXinService;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import org.apache.commons.beanutils.BeanUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

/**
 * Created by wcg on 16/3/17.
 */
@RestController
@RequestMapping("/merchant")
public class MerchantController {

  @Inject
  private MerchantService merchantService;

  @Inject
  private WeiXinService weiXinService;

  @RequestMapping("/index")
  public ModelAndView goMerchantPage(HttpServletRequest request, Model model) {
    model.addAttribute("wxConfig", weiXinService.getWeiXinConfig(request));
    return MvUtil.go("/weixin/merchant");
  }

  @RequestMapping("/type")
  public ModelAndView goMerchantTypePage(@RequestParam(required = false) String cityName,
                                         @RequestParam(required = false) Integer status,
                                         @RequestParam(required = false) Integer condition,
                                         @RequestParam(required = false) Long type,
                                         @RequestParam(required = false) Double lat,
                                         @RequestParam(required = false) Double lon, Model model) {
    model.addAttribute("cityName", cityName);
    if (condition == null) {
      condition = 0;
    }
    model.addAttribute("condition", condition);
    model.addAttribute("status", status);
    model.addAttribute("type", type);
    model.addAttribute("lat", lat);
    model.addAttribute("lon", lon);
    return MvUtil.go("/weixin/merchantType");
  }

  //分页
  @RequestMapping(value = "/list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
  public
  @ResponseBody
  List<MerchantDto> findPageMerchant(
      @RequestParam(value = "page", required = false) Integer offset,
      @RequestParam(required = false) String cityName,
      @RequestParam(required = false) Integer status, @RequestParam(required = false) Long type,
      @RequestParam(required = false) Integer condition,
      @RequestParam(required = false) Double lat, @RequestParam(required = false) Double lon) {
//    return merchantService.findMerchantsByPage(offset);
    if (offset == null) {
      offset = 1;
    }
    List<MerchantDto>
        merchantDtoList =
        merchantService
            .findWxMerchantListByCustomCondition(status, lat, lon, offset, type, cityName,
                                                 condition);
    return merchantDtoList;
  }

  @RequestMapping(value = "/info/{id}", method = RequestMethod.GET)
  public ModelAndView goMerchantPage(HttpServletRequest request, Model model,
                                     @PathVariable Long id,
                                     @RequestParam(required = false) String distance,
                                     @RequestParam(required = false) Integer status) {
    Merchant merchant = merchantService.findMerchantById(id);
    List<MerchantScroll> scrolls = merchantService.findAllScorllPicture(merchant);
    model.addAttribute("merchant", merchant);
    if (scrolls == null || scrolls.size() < 1) {
      model.addAttribute("hasScroll", 0);
    } else {
      model.addAttribute("hasScroll", 1);
    }
    model.addAttribute("scrolls", scrolls);

    model.addAttribute("distance", distance);
    model.addAttribute("status", status);
    model.addAttribute("wxConfig", weiXinService.getWeiXinConfig(request));
    return MvUtil.go("/weixin/merchantInfo");
  }

  @ApiOperation(value = "首页加载商家列表及周边1.0")
  @RequestMapping(value = "/reload", method = RequestMethod.POST)
  public
  @ResponseBody
  LejiaResult reload(
      @ApiParam(value = "经度(保留六位小数)") @RequestParam(required = false) Double longitude,
      @ApiParam(value = "纬度(保留六位小数)") @RequestParam(required = false) Double latitude,
      @ApiParam(value = "第几页") @RequestParam(required = false) Integer page,
      @ApiParam(value = "商家类别") @RequestParam(required = false) Long type,
      @ApiParam(value = "城市id") @RequestParam(required = false) Long cityId,
      @ApiParam(value = "某个城市的区域id") @RequestParam(required = false) Long areaId) {

    List<MerchantDto>
        merchantDtoList =
        merchantService
            .findMerchantListByCustomCondition(latitude, longitude, page, type, cityId, areaId);

    return LejiaResult.build(200, "ok", merchantDtoList);
  }

  @ApiOperation(value = "进入商家详情页")
  @RequestMapping(value = "/detail", method = RequestMethod.POST)
  public
  @ResponseBody
  LejiaResult detail(@ApiParam(value = "商家Id") @RequestParam(required = true) Long id) {
    MerchantDto merchantDto = new MerchantDto();
    Merchant merchant = merchantService.findMerchantById(id);
    List<MerchantDetail> detailList = merchantService.findAllMerchantDetailByMerchant(merchant);
    try {
      BeanUtils.copyProperties(merchantDto, merchant);
      merchantDto.setDetailList(detailList);
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
    return LejiaResult.build(200, "ok", merchantDto);
  }

}
