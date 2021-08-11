package com.wjl.task;

import cn.hutool.http.HttpUtil;
import com.wjl.util.AliDns;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 修改域名定时任务
 *
 * @author wangJiaLun
 * @date 2021-08-11
 **/
@Slf4j
@Component
public class ModifyDomainNameTask {

    @Autowired
    private AliDns aliDns;

    @Value("${pub-network-address}")
    private String netWorkAddress;

    /**
     *  每五分钟获取公网ip去尝试更新域名解析记录
     */
    @Scheduled(cron = "0 0/5 * * * ? ")
    public void checkDomainNameValue(){
        String value = HttpUtil.get(netWorkAddress).replace("\n", "");
        try {
            aliDns.updateDomainRecord(value);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

}
