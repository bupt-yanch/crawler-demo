# crawler-demo
crawl4j+mysql 实现简单爬取+存储

## 现状
- 可定时爬取
- 查重做的不好
  - 目前用mysql的unique key进行去重，效率较低
  - 之后可以使用redis维护查询过url集合
