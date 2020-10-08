package cn.cf.sharding.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.shardingsphere.api.config.sharding.ShardingRuleConfiguration;
import org.apache.shardingsphere.api.config.sharding.TableRuleConfiguration;
import org.apache.shardingsphere.api.config.sharding.strategy.InlineShardingStrategyConfiguration;
import org.apache.shardingsphere.shardingjdbc.api.ShardingDataSourceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author miaomiao
 * @date 2020/10/8 21:29
 */
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
