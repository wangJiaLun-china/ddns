## 解决公网ip经常变动

### 问题

​	家里申请了电信的公网ip, 但是如果停电或者重新拨号都会导致这个ip变更.很多配置好的应用都需要重新配置ip地址太麻烦了

### 思考

- 现成的产品类似花生壳这些内网穿透都要收费, 我已有公网ip感觉不划算

- 想到了域名配置dns解析, 应用里面都配置域名代替ip地址.每次ip变了我只需要去改一下域名的dns解析就可以了

  但是还会有问题, 每次都需要手动改一下, 并且还必须得连上家里网才能知道最新的公网ip

- 查了阿里云/腾讯云/华为云等等dns解析都对外提供了`api`文档, 思路是写个定时脚本获取到公网ip再与阿里云上的解析记录做比对,

  阿里云解析记录不是当前的公网ip的话, 更新阿里云的解析记录

<!--more-->

### 实现

- 定时获取当前公网ip去尝试更新

  ```java
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
  ```

- 尝试更新阿里云解析记录

    ```java
    /**
     * 阿里云ddns配置
     *
     * @author wangJiaLun
     * @date 2021-08-11
     **/
    @Slf4j
    @Component
    public class AliDns {
    
        @Value("${aliddns.access-key-id}")
        String ACCESS_KEY_ID ;
        @Value("${aliddns.access-key-secret}")
        String ACCESS_KEY_SECRET;
        @Value("${aliddns.domain-name}")
        String  DOMAIN_NAME;
        @Value("${aliddns.end-point}")
        String END_POINT;
    
        /**
         * 使用AK&SK初始化账号Client
         * @param accessKeyId
         * @param accessKeySecret
         * @return Client
         * @throws Exception
         */
        com.aliyun.alidns20150109.Client createClient(String accessKeyId, String accessKeySecret) throws Exception {
            Config config = new Config()
                    .setAccessKeyId(accessKeyId)Secret
                    .setAccessKeySecret(accessKeySecret);
            config.endpoint = END_POINT;
            return new com.aliyun.alidns20150109.Client(config);
        }
    
        /**
         *  更新解析记录
         * @param value 当前公网ip
         */
        public  void updateDomainRecord (String value) throws Exception{
            com.aliyun.alidns20150109.Client client = this.createClient(ACCESS_KEY_ID, ACCESS_KEY_SECRET);
    
            DescribeDomainRecordsRequest domainRecordsRequest = new DescribeDomainRecordsRequest();
            domainRecordsRequest.setDomainName(DOMAIN_NAME);
            // 解析记录列表
            DescribeDomainRecordsResponse domainRecordsResponse = client.describeDomainRecords(domainRecordsRequest);
            for (DescribeDomainRecordsResponseBody.DescribeDomainRecordsResponseBodyDomainRecordsRecord record : domainRecordsResponse.body.domainRecords.record) {
                UpdateDomainRecordRequest updateDomainRecordRequest = new UpdateDomainRecordRequest();
                if (record.value.equals(value)) {
                    break;
                }
                BeanUtils.copyProperties(record, updateDomainRecordRequest);
                updateDomainRecordRequest.setValue(value);
                client.updateDomainRecord(updateDomainRecordRequest);
                log.info("域名:{}的主机记录{}更新为记录值: {}",DOMAIN_NAME, record.RR,value);
            }
        }
    }
    ```

### 测试

去阿里云平台上面把域名的解析记录随便改了个值, 过几分钟后变成了我当前的公网ip地址

### 项目地址

https://github.com/wangJiaLun-china/ddns
