### 一、核心概念
传统将数据集中存储在单一节点的解决方案的不足：
1.性能：由于关系型数据库大多采用B+树索引，在数据量超过阈值情况下，索引深度的增加也将使得磁盘访问IO次数增加，进而导致查询性能得下降，同时高并发请求也使得集中式数据库成为系统最大瓶颈
2.可用性:
3.运维：当单一数据库实例得数据达到阈值之后，数据得备份和恢复的时间成本都会随着数据量大小而愈发不可控

**数据分片是指**：按照某个维度将存放在单一数据库中的数据**分散**存放在多个数据库或表中以达到提升性能瓶颈以及可用性的效果。数据分片分为**垂直分片**和**水平分片**

**垂直分片**：按照业务拆分的方式称为垂直分片，又称纵向拆分，核心理念是专库专用。垂直拆分可以缓解数据量和访问量带来的问题，但是无法根治

**水平分片**：又称为横向分片，不根据业务逻辑划分，而是通过某个字段（某些字段）根据某种规则将数据分散至多个库或表中，每个分片仅包含数据的一部分。


#### 1.1数据分片核心概念
##### 1.1.1SQL
- 逻辑表
- 真实表
- 数据节点
- 绑定表
  如果主表和子表之间不设置为绑定表，则查询过程将呈现为笛卡儿积关联，否则子表和主表在进行路由时，所有的路由计算将只使用主表的策略

##### 1.1.2分片
- 分片键
  用于分片的数据库字段
- 分片算法
> 1.精确分片算法：PreciseShardingAlgorithm，用于处理使用单一键作为分片键的与In进行分片的场景，配合StandardShardingStrategy使用

> 2.复合分片：对应ComplexKeysShardingAlgorithm，用于处理使用多键作为分片键进行分片的场景，配合ComplexShardingStrategy使用

> 3.范围分片:对应RangeShardingAlgorithm，用于处理使用单一键作为 分片键的BETWEEN AND、>、<、<=、>=进行分片的场景，配合StandardShardingStrategy使用

> 4.Hint分片：对应HintShardingAlgorithm，用于处理使用Hint行分片的场景，配置配合HintShardingStrategy使用

- 分片策略
  包含分片键和分片算法
> 1.标准分片策略：对应StandardShardingStrategy。提供对SQL语句中的=, >, <, >=, <=, IN和BETWEEN AND的分片操作支持

> 2.复合分片策略

> 3.行表达式分片策略，只支持单分片键。

> 4.Hint分片策略

> 5.不分片策略

#### 1.2 数据分片内核剖析
![image.png](https://upload-images.jianshu.io/upload_images/6922386-9c8b583a0f7f6196.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


### 二、代码实践
#### 2.1 准备工作
##### 2.1.1 建库建表
ds0
```
CREATE DATABASE `ds0`;

USE `ds0`;

CREATE TABLE `user0` (
  `user_id` BIGINT(20) NOT NULL,
  `user_name` VARCHAR(200) DEFAULT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8;


CREATE TABLE `user1` (
  `user_id` BIGINT(20) NOT NULL,
  `user_name` VARCHAR(200) DEFAULT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8;

```
ds1
```


CREATE DATABASE `ds1`;

USE `ds1`;

CREATE TABLE `user0` (
  `user_id` BIGINT(20) NOT NULL,
  `user_name` VARCHAR(200) DEFAULT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8;


CREATE TABLE `user1` (
  `user_id` BIGINT(20) NOT NULL,
  `user_name` VARCHAR(200) DEFAULT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8;

```
##### 2.1.2 引入pom
```
        <dependency>
            <groupId>org.apache.shardingsphere</groupId>
            <artifactId>sharding-jdbc-spring-boot-starter</artifactId>
            <version>4.1.0</version>
        </dependency>
```
#### 2.2 代码实践
#####  2.2.1 配置指导(这里通过properties配置进行示例)
###### 2.2.1.1 分库分表多数据源信息配置
```yaml
spring.shardingsphere.datasource.names= #数据源名称，多数据源以逗号分隔

spring.shardingsphere.datasource.<data-source-name>.type= #数据库连接池类名称
spring.shardingsphere.datasource.<data-source-name>.driver-class-name= #数据库驱动类名
spring.shardingsphere.datasource.<data-source-name>.url= #数据库url连接
spring.shardingsphere.datasource.<data-source-name>.username= #数据库用户名
spring.shardingsphere.datasource.<data-source-name>.password= #数据库密码
spring.shardingsphere.datasource.<data-source-name>.xxx= #数据库连接池的其它属性
```
###### 2.2.1.2 分片策略和分片算法配置
**配置具体表的数据节点**
```
spring.shardingsphere.sharding.tables.<logic-table-name>.actual-data-nodes= #由数据源名 + 表名组成，以小数点分隔。多个表以逗号分隔，支持inline表达式。缺省表示使用已知数据源与逻辑表名称生成数据节点，用于广播表（即每个库中都需要一个同样的表用于关联查询，多为字典表）或只分库不分表且所有库的表结构完全一致的情况
```
**为表和库分别配置分片策略和算法：**
共有四种策略可以配置，分库和分表各自只能选其一进行配置
- 单分片键
```yaml
spring.shardingsphere.sharding.tables.<logic-table-name>.database-strategy.standard.sharding-column= #分片列名称
spring.shardingsphere.sharding.tables.<logic-table-name>.database-strategy.standard.precise-algorithm-class-name= #精确分片算法类名称，用于=和IN。该类需实现PreciseShardingAlgorithm接口并提供无参数的构造器
spring.shardingsphere.sharding.tables.<logic-table-name>.database-strategy.standard.range-algorithm-class-name= #范围分片算法类名称，用于BETWEEN，可选。该类需实现RangeShardingAlgorithm接口并提供无参数的构造器
```
- 多分片键的复合分片
```yaml
spring.shardingsphere.sharding.tables.<logic-table-name>.database-strategy.complex.sharding-columns= #分片列名称，多个列以逗号分隔
spring.shardingsphere.sharding.tables.<logic-table-name>.database-strategy.complex.algorithm-class-name= #复合分片算法类名称。该类需实现ComplexKeysShardingAlgorithm接口并提供无参数的构造器
```
- 行表达式分片
```yaml
spring.shardingsphere.sharding.tables.<logic-table-name>.database-strategy.inline.sharding-column= #分片列名称
spring.shardingsphere.sharding.tables.<logic-table-name>.database-strategy.inline.algorithm-expression= #分片算法行表达式，需符合groovy语法
```
- Hint分片策略
```yaml
spring.shardingsphere.sharding.tables.<logic-table-name>.database-strategy.hint.algorithm-class-name= #Hint分片算法类名称。该类需实现HintShardingAlgorithm接口并提供无参数的构造器
```
**配置未设置分片规则的表路由的默认数据源以及默认分库和分表的策略**
```yaml
spring.shardingsphere.sharding.default-data-source-name= #未配置分片规则的表将通过默认数据源定位
spring.shardingsphere.sharding.default-database-strategy.xxx= #默认数据库分片策略，同分库策略
spring.shardingsphere.sharding.default-table-strategy.xxx= #默认表分片策略，同分表策略
```
###### 2.2.1.3 配置绑定表和广播表
binding-tables和broadcast-tables为集合
```yaml
spring.shardingsphere.sharding.binding-tables[0]= #绑定表规则列表
spring.shardingsphere.sharding.binding-tables[1]= #绑定表规则列表
spring.shardingsphere.sharding.binding-tables[x]= #绑定表规则列表

spring.shardingsphere.sharding.broadcast-tables[0]= #广播表规则列表
spring.shardingsphere.sharding.broadcast-tables[1]= #广播表规则列表
spring.shardingsphere.sharding.broadcast-tables[x]= #广播表规则列表
```

###### 2.2.1.4 其他配置
- 配置分布式自增主键策略
```
spring.shardingsphere.sharding.tables.<logic-table-name>.key-generator.column= #自增列名称，缺省表示不使用自增主键生成器
spring.shardingsphere.sharding.tables.<logic-table-name>.key-generator.type= #自增列值生成器类型，缺省表示使用默认自增列值生成器。可使用用户自定义的列值生成器或选择内置类型：SNOWFLAKE/UUID
spring.shardingsphere.sharding.tables.<logic-table-name>.key-generator.props.<property-name>= #属性配置, 注意：使用SNOWFLAKE算法，需要配置worker.id与max.tolerate.time.difference.milliseconds属性。若使用此算法生成值作分片值，建议配置max.vibration.offset属性
```
- 配置开启显示SQL解析过程
```
spring.shardingsphere.props.sql.show= #是否开启SQL显示，默认值: false
```
##### 2.2.2 分库分表配置实战（Yaml配置方式）
这里分库和分表的分片策略均采用行表达式，分片算法均为针对user_id 对2进行取模路由

```yaml
spring:
  shardingsphere:
    #配置数据源名称，多数据源以逗号进行分隔
    datasource:
      names: ds0,ds1
    #配置各个数据源的基本信息
      ds0:
        # type:数据库连接池名称
        type: com.alibaba.druid.pool.DruidDataSource
        driver-class-name: com.mysql.jdbc.Driver
        url: jdbc:mysql://localhost:3306/ds0?serverTimezone=UTC
        username: root
        password: 1234
      ds1:
        type: com.alibaba.druid.pool.DruidDataSource
        driver-class-name: com.mysql.jdbc.Driver
        url: jdbc:mysql://localhost:3306/ds1?serverTimezone=UTC
        username: root
        password: 1234
    sharding:
      tables:
        user:
          actual-data-nodes: ds$->{0..1}.user$->{0..1}
          # 配置分库策略
          database-strategy:
            inline:
              sharding-column: user_id #配置分片键名称
              algorithm-expression: ds$->{user_id % 2} #配置分片算法行表达式
          # 配置分表策略
          table-strategy:
             inline:
              sharding-column: user_id #配置分片键名称
              algorithm-expression: user$->{user_id % 2} #配置分片算法行表达式
    props:
      sql:
        show: true
```
##### 2.2.2 分库分表配置实战（Java代码配置方式）
真实业务中肯定很多业务表需要进行分表，当然不可能每张表都通过yaml方式去配置，所以最终可能还是需要通过java代码进行分库分表的配置。下述代码功能将和上述yaml的配置相同。

pom
```
        <dependency>
            <groupId>org.apache.shardingsphere</groupId>
            <artifactId>sharding-jdbc-core</artifactId>
            <version>4.0.1</version>
        </dependency>
```
配置代码
```java
@Configuration
public class ShardingDataSourceConfiguration {
    @Bean
    public DataSource getShardingDataSource() throws SQLException {
        ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();
        // 获取user表的分片规则配置
        TableRuleConfiguration userInfoTableRuleConfiguration = getUserInfoTableRuleConfiguration();

        shardingRuleConfig.getTableRuleConfigs().add(userInfoTableRuleConfiguration);
        return ShardingDataSourceFactory.createDataSource(createDataSourceMap(), shardingRuleConfig, new Properties());
    }

    /**
     * 配置真实数据源
     * @return 数据源map
     */
    private Map<String, DataSource> createDataSourceMap() {
        Map<String, DataSource> dataSourceMap = new HashMap<>();
        DruidDataSource druidDataSource1 = new DruidDataSource();
        druidDataSource1.setUrl("jdbc:mysql://localhost:3306/ds0?serverTimezone=UTC");
        druidDataSource1.setUsername("root");
        druidDataSource1.setDriverClassName("com.mysql.jdbc.Driver");
        druidDataSource1.setPassword("1234");

        DruidDataSource druidDataSource2 = new DruidDataSource();
        druidDataSource2.setUrl("jdbc:mysql://localhost:3306/ds1?serverTimezone=UTC");
        druidDataSource2.setUsername("root");
        druidDataSource2.setDriverClassName("com.mysql.jdbc.Driver");
        druidDataSource2.setPassword("1234");

        dataSourceMap.put("ds0",druidDataSource1);
        dataSourceMap.put("ds1",druidDataSource2);
        return dataSourceMap;
    }

    /**
     * 配置user表的分片规则
     *
     * @return ser表的分片规则配置对象
     */
    private TableRuleConfiguration getUserInfoTableRuleConfiguration() {

        // 为user表配置数据节点
        TableRuleConfiguration ruleConfiguration = new TableRuleConfiguration("user", "ds${0..1}.user${0..1}");
        // 设置分片键
        String shardingKey = "user_id";
        // 为user表配置分库分片策略及分片算法
        ruleConfiguration.setDatabaseShardingStrategyConfig(new InlineShardingStrategyConfiguration(shardingKey, "ds${user_id % 2}"));
        // 为user表配置分表分片策略及分片算法
        ruleConfiguration.setTableShardingStrategyConfig(new InlineShardingStrategyConfiguration(shardingKey, "user${user_id % 2}"));

        return ruleConfiguration;
    }

}

```

**完整实战代码github:https://github.com/xiaomaomiao/ShardingSphereDemo.git**

#### 2.3、踩坑记录
#### 1.ShardingShpere配合Druid数据源启动报错
Failed to configure a DataSource: 'url' attribute is not specified and no embedded datasource could be configured.

https://www.freesion.com/article/2653987797/